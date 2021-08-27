package spright8;

import java.util.Iterator;

import battlecode.common.*;


public class Slanderer extends Robot{
	
	/*
	 *New priorities:
	 * 1. move away from closest EM
	 * 2. move away from friendly sensing EM
	 * 3. move away from closest EEC
	 * 4. move away from closest FEC
	 * 5. move away from closest FS
	 */
	
	
	//things to avoid
	public MapLocation closestEM = null;
	public final int EM_DIST_TOLERANCE = 400;
	public MapLocation closestEEC = null;
	public final int EEC_DIST_TOLERANCE = 400;
	public MapLocation closestFEC = null;
	public MapLocation closestFS = null;
	public MapLocation FsensingEM = null;
	public final int FS_DIST_TOLERANCE = 10;
	
	//Flag variables
	public int sensedE;
	public int bestFlag;
	public int sensedFlag;
	public int sensedFlagID;
	
	//sensed robot information
	public RobotType riType;
	public Team riTeam;
	public int riID;
	
	public Slanderer(RobotController rc) {
		super(rc);
	}
	
	public void takeTurn() throws GameActionException{
		super.takeTurn();
		
		//reset sense only every round
		closestFEC = null;
		closestFS = null;
		FsensingEM = null;
		
		possibleFlagsToEcho.clear();
		bestFlag = 0;
		sensedE = 0;
		
		for(RobotInfo ri : sensedRobots) {
			riType = ri.getType();
			riTeam = ri.getTeam();
			if(riTeam == team) {
				riID = ri.getID();
				switch(riType) {
				case ENLIGHTENMENT_CENTER:
					fecids.add(riID);
					closestFEC = getClosest(closestFEC, ri.getLocation());
					break;
				case MUCKRAKER:
					break;
				case POLITICIAN:
					if(rc.canGetFlag(riID) && getFlagSlanderer(rc.getFlag(riID))) {
						closestFS = getClosest(closestFS, ri.getLocation());
					}
				case SLANDERER:
				default:
					break;
				}
				//read team mates flag for info
				if(rc.canGetFlag(riID)) {
					sensedFlag = rc.getFlag(riID);
					sensedFlagID = getFlagID(sensedFlag);
					if(sensedFlagID == REPORT_EM && !currentLocation.isWithinDistanceSquared(getLocationFromFlag(sensedFlag), type.sensorRadiusSquared)) {
						closestEM = getClosest(closestEM, getLocationFromFlag(sensedFlag));
					}else if(sensedFlagID == REPORT_EEC && !currentLocation.isWithinDistanceSquared(getLocationFromFlag(sensedFlag), type.sensorRadiusSquared)) {
						closestEEC = getClosest(closestEEC, getLocationFromFlag(sensedFlag));
					}else if(sensedFlagID == REPORT_FEC_ID) {
						fecids.add(getFlagFECID(sensedFlag));
					}
					
					if(sensedFlagID != REPORT_NEC && sensedFlagID != REPORT_EEC && sensedFlagID != REPORT_FEC_ID && getFlagNearbyEM(sensedFlag)) {
						FsensingEM = ri.getLocation();
					}
					if(sensedFlagID == REPORT_EEC || sensedFlagID == REPORT_NEC) {
						possibleFlagsToEcho.add(sensedFlag & MASK_PERSONAL_EC);
					}else if(sensedFlagID != REPORT_FEC_ID){
						possibleFlagsToEcho.add(sensedFlag & MASK_PERSONAL);
					}
					
				}
			}else {
				switch(riType) {
				case ENLIGHTENMENT_CENTER:
					if(riTeam == enemyTeam) {
						if(REPORT_EEC > getFlagID(bestFlag)) {
							bestFlag = createFlag(REPORT_EEC,1, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
						}
						closestEEC = getClosest(closestEEC, ri.getLocation());
					}else if(riTeam == Team.NEUTRAL && REPORT_NEC > getFlagID(bestFlag)){
						bestFlag = createFlag(REPORT_NEC,1, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
					}
					break;
				case MUCKRAKER:
					if(REPORT_EM > getFlagID(bestFlag)) {
						bestFlag = createFlag(REPORT_EM,1,0,ri.getLocation());
					}
					sensedE = sensedE | MASK_IS_EM;
					closestEM = getClosest(closestEM, ri.getLocation());
					break;
				case POLITICIAN:
					if(REPORT_EP > getFlagID(bestFlag)){
						bestFlag = createFlag(REPORT_EPS,1,0,ri.getLocation());
					}
					sensedE = sensedE | MASK_IS_EP;
					break;
				case SLANDERER:
					break;
				default:
					break;
				}	
			}
		}
		
		for(Iterator<Integer> iter = fecids.iterator(); iter.hasNext();){
        	riID = iter.next();
        	if(rc.canGetFlag(riID)) {
        		sensedFlag = rc.getFlag(riID);
				sensedFlagID = getFlagID(sensedFlag);
				if(sensedFlagID == REPORT_EM && !currentLocation.isWithinDistanceSquared(getLocationFromFlag(sensedFlag), type.sensorRadiusSquared)) {
					closestEM = getClosest(closestEM, getLocationFromFlag(sensedFlag));
				}else if(sensedFlagID == REPORT_EEC && !currentLocation.isWithinDistanceSquared(getLocationFromFlag(sensedFlag), type.sensorRadiusSquared)) {
					closestEEC = getClosest(closestEEC, getLocationFromFlag(sensedFlag));
				}else if(sensedFlagID == REPORT_FEC_ID) {
					fecids.add(getFlagFECID(sensedFlag));
				}
				
				if(sensedFlagID == REPORT_EEC || sensedFlagID == REPORT_NEC) {
					possibleFlagsToEcho.add(sensedFlag & MASK_PERSONAL_EC);
				}else if(sensedFlagID != REPORT_FEC_ID){
					possibleFlagsToEcho.add(sensedFlag & MASK_PERSONAL);
				}
	  		}else {
	  			iter.remove();
	  		}
        }
		
		//no enemy sensed to report, get best flag to echo
		if(bestFlag == 0) {
			for(Iterator<Integer> iter = possibleFlagsToEcho.iterator(); iter.hasNext();){
	        	sensedFlag = iter.next();
	        	bestFlag = getClosest(bestFlag, sensedFlag);
	        }
		}
		
		if(roundNum % ECHO_WIPE_RATE == 0) {
			bestFlag = 0;
		}
		
		//no flag to echo, report FEC id
		if(bestFlag == 0) {
			int randomFECindex = (int)(Math.random()*fecids.size());
			int i = 0;
			for(Iterator<Integer> iter = fecids.iterator(); iter.hasNext();){
				riID = iter.next();
	        	if(i == randomFECindex) {
	        		bestFlag = createFlag(REPORT_FEC_ID,1,riID);
	        		break;
	        	}
	        	i++;
	        }
		}
		
		//add personal info to bestFlag
		//set as slanderer
		bestFlag = bestFlag | MASK_IS_SLAND;
		sensedFlagID = getFlagID(bestFlag);
		//set sensing EM,EP,ES bits if unit report
		if(sensedFlagID != REPORT_NEC && sensedFlagID != REPORT_EEC && sensedFlagID != REPORT_FEC_ID) {
			bestFlag = bestFlag | sensedE;
		}
		
		//set flag
		if(rc.canSetFlag(bestFlag)) {
			rc.setFlag(bestFlag);
		}
		
		//if closestEM or closestEEC are outside of tolerance, ignore
		if(closestEM != null && !currentLocation.isWithinDistanceSquared(closestEM, EM_DIST_TOLERANCE)){
			closestEM = null;
		}
		if(closestEEC != null && !currentLocation.isWithinDistanceSquared(closestEEC, EEC_DIST_TOLERANCE)){
			closestEEC = null;
		}
		
		//move
		if(rc.isReady()) {
			Direction bestDirection = null;
			
			//1. move away from EM
			if(closestEM != null) {
				bestDirection = getBestValidDirection(closestEM.directionTo(currentLocation));
			}
			
			//2. move away from friendly sensing EM
			
			else if(FsensingEM != null) {
				bestDirection = getBestValidDirection(FsensingEM.directionTo(currentLocation));
			}
			
			//3. move away from closest EEC
			else if(closestEEC != null) {
				bestDirection = getBestValidDirection(closestEEC.directionTo(currentLocation));
			}
			
			//4. move away from closest FEC
			else if(closestFEC != null) {
				bestDirection = getBestValidDirection(closestFEC.directionTo(currentLocation));
			}
			
			//5. move away from closest FS
			else if(closestFS != null) {
				bestDirection = getBestValidDirection(closestFS.directionTo(currentLocation));
			}
			
			if(bestDirection != null && rc.canMove(bestDirection)) {
				rc.move(bestDirection);
			}
		}
			
	}
	
}