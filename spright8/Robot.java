package spright8;

import java.util.HashSet;

import battlecode.common.*;

public class Robot {
	
	public RobotController rc;
	public Team team;
	public Team enemyTeam;
	public RobotType type;
	MapLocation currentLocation;
	public RobotInfo[] sensedRobots;
	public int turnCount = 0;
	public int roundNum;
	public HashSet<Integer> fecids;
	public HashSet<Integer> possibleFlagsToEcho;
	
	public final int ECHO_WIPE_RATE = 3;
	
	//public final int ECHO_WIPE_RATE = 5;
	
	public final int MASK_4 = 15;
	public final int MASK_7 = 127;
	public final int MASK_19 = 524287;
	public final int MASK_IS_SLAND = 524288;
	public final int MASK_IS_ES = 16384;
	public final int MASK_IS_EP = 32768;
	public final int MASK_IS_EM = 65536;
	public final int MASK_PERSONAL_EC = 16252927;
	public final int MASK_PERSONAL = 15745023;
	
	public final Direction[] directions = {
        Direction.NORTH,
        Direction.SOUTH,
        Direction.EAST,
        Direction.WEST,
        Direction.NORTHEAST,
        Direction.SOUTHWEST,
        Direction.SOUTHEAST,
        Direction.NORTHWEST,
    };
	//Communication. Message id in order of priority.
	public final int REPORT_ES = 15;//4 bit message id, 1 bit isSland,2 not used bits,3 bit isEM/EP/ES, 7 bit x%128, 7 bit y%128 = 24 bits
	public final int REPORT_NEC = 14;//4 bit message id, 1 bit isSland,1 not used bit,4 bit 50's conviction, 7 bit x%128, 7 bit y%128 = 24 bits
	public final int REPORT_EEC = 13;
	public final int REPORT_EPS = 12;
	public final int REPORT_EP = 11;
	public final int REPORT_EM = 10;
	public final int REPORT_FEC_ID = 9;//4 bit message id, 1 bit isSland,19 bit FEC id = 24 bits
	
	public Robot(RobotController rc) {
		this.rc = rc;
		team = rc.getTeam();
		enemyTeam = team.opponent();
		type = rc.getType();
		roundNum = rc.getRoundNum();
		fecids = new HashSet<Integer>();
		possibleFlagsToEcho = new HashSet<Integer>();
	}
	
	public Robot(RobotController rc, Robot r) {
		this.rc = rc;
		team = rc.getTeam();
		enemyTeam = team.opponent();
		type = rc.getType();
		roundNum = rc.getRoundNum();
		fecids = r.fecids;
		turnCount = r.turnCount;
		possibleFlagsToEcho = r.possibleFlagsToEcho;
	}
    
	public void takeTurn() throws GameActionException{
		sensedRobots = rc.senseNearbyRobots();
		turnCount++;
		roundNum = rc.getRoundNum();
		currentLocation = rc.getLocation();
	}
	
	public int getFlagID(int flag) {
		return (flag >> 20);
	}
	public int getFlagX(int flag) {
		return (flag >> 7) & MASK_7;
	}
	public int getFlagY(int flag) {
		return flag & MASK_7;
	}
	public int getFlagFECID(int flag) {
		return flag & MASK_19;
	}
	public int getFlagConviction(int flag) {
		return (flag >> 14) & MASK_4;
	}
	public boolean getFlagSlanderer(int flag) {
		return (flag & MASK_IS_SLAND) > 0;
	}
	public boolean getFlagNearbyEM(int flag) {
		return (flag & MASK_IS_EM) > 0;
	}
	public boolean getFlagNearbyEP(int flag) {
		return (flag & MASK_IS_EP) > 0;
	}
	public boolean getFlagNearbyES(int flag) {
		return (flag & MASK_IS_ES) > 0;
	}
	
	public int createFlag(int id, int isSland, int extra, MapLocation loc) {
		return (((((((id << 1) | isSland) << 5) | extra) << 7) | (loc.x % 128)) << 7) | (loc.y % 128);
	}
	
	public int createFlag(int id, int isSland, int extra) {
		return (((id << 1) | isSland) << 19) | extra;
	}
	
	public MapLocation getClosest(MapLocation a, MapLocation b) {
		if(a == null && b == null) {
			return null;
		}else if(a == null) {
			return b;
		}else if(b == null) {
			return a;
		}
		if(currentLocation.distanceSquaredTo(a) < currentLocation.distanceSquaredTo(b)) {
			return a;
		}
		return b;
	}
	
	public int getClosest(int flagA, int flagB) {
		if(flagA == 0 && flagB == 0) {
			return flagA;
		}
		int distSquareToB = currentLocation.distanceSquaredTo(getLocationFromFlag(flagB));
		if(flagA == 0) {
			if(distSquareToB > type.sensorRadiusSquared) {
				return flagB;
			}
			return flagA;
		}
		int distSquareToA = currentLocation.distanceSquaredTo(getLocationFromFlag(flagA));
		if(flagB == 0) {
			if(distSquareToA > type.sensorRadiusSquared) {
				return flagA;
			}
			return flagB;
		}
		
		if(distSquareToA < distSquareToB && distSquareToA > type.sensorRadiusSquared) {
			return flagA;
		}
		if(distSquareToB > type.sensorRadiusSquared) {
			return flagB;
		}
		return 0;
	}
	
	public MapLocation getLocationFromFlag(int flag) {
		int curX = currentLocation.x;
		int curY = currentLocation.y;
		
		int offsetX128 = curX / 128;
		int offsetY128 = curY / 128;
		
		int guessX = offsetX128 * 128 + getFlagX(flag);
		int guessY = offsetY128 * 128 + getFlagY(flag);
		
		if(curX - guessX > 64) {
			guessX += 128;
		}else if(curX - guessX < -64) {
			guessX -= 128;
		}
		
		if(curY - guessY > 64) {
			guessY += 128;
		}else if(curY - guessY < -64) {
			guessY -= 128;
		}
		
		return new MapLocation(guessX,guessY);
	}
	
	public Direction getBestValidDirection(Direction toward) throws GameActionException{
		MapLocation ac = currentLocation.add(toward);
		MapLocation al = currentLocation.add(toward.rotateLeft());
		MapLocation ar = currentLocation.add(toward.rotateRight());
		int passC = 0;
		int passL = 0;
		int passR = 0;
		boolean canMoveC = rc.canMove(toward);
		boolean canMoveL = rc.canMove(toward.rotateLeft());
		boolean canMoveR = rc.canMove(toward.rotateRight());
		if(rc.canSenseLocation(ac)) {
			passC = (int)(100*rc.sensePassability(ac));
		}
		if(rc.canSenseLocation(al)) {
			passL = (int)(100*rc.sensePassability(al));
		}
		if(rc.canSenseLocation(ar)) {
			passR = (int)(100*rc.sensePassability(ar));
		}
		if(canMoveC && canMoveL && canMoveR){
			if(passC >= passL && passC >= passR) {
				return toward;
			}else if(passL >= passR) {
				return toward.rotateLeft();
			}
			return toward.rotateRight();
		}
		if(canMoveC && canMoveL){
			if(passC >= passL) {
				return toward;
			}
			return toward.rotateLeft();
		}
		if(canMoveC && canMoveR){
			if(passC >= passR) {
				return toward;
			}
			return toward.rotateRight();
		}
		if(canMoveR && canMoveL) {
			if(passL >= passR) {
				return toward.rotateLeft();
			}
			return toward.rotateRight();
		}
		if(canMoveC) {
			return toward;
		}
		if(canMoveL) {
			return toward.rotateLeft();
		}
		if(canMoveR) {
			return toward.rotateRight();
		}
		if(rc.canMove(toward.rotateRight().rotateRight())) {
			return toward.rotateRight().rotateRight();
		}
		if(rc.canMove(toward.rotateLeft().rotateLeft())) {
			return toward.rotateLeft().rotateLeft();
		}
		
		return null;
	}
	public Direction getBestValidDirection(MapLocation t) throws GameActionException{
		if(t == null) {
			double highestPassability = 0;
			Direction highestPassabilityDirection = Direction.SOUTH;
			MapLocation tempLocation = null;
			for(Direction dir : directions) {
				tempLocation = currentLocation.add(dir);
				if(rc.canSenseLocation(tempLocation) && rc.sensePassability(tempLocation) > highestPassability && rc.canMove(dir)) {
					highestPassabilityDirection = dir;
					highestPassability = rc.sensePassability(tempLocation);
				}
			}
			return highestPassabilityDirection;
		}
		return getBestValidDirection(currentLocation.directionTo(t));
		
	}
	
	
	public int getHeadingIndex(Direction dir) {
		if(dir == null) {
			return -1;
		}
		for(int i = 0; i < directions.length; i++) {
			if(dir.equals(directions[i])) {
				return i;
			}
		}
		return -1;
	}
	
}