package spright8;

import java.util.Iterator;

import battlecode.common.*;


public class Muckraker extends Robot{
	
	/*
	 *New priorities:
	 * 1. expose ES with highest influence
	 * 2. move away from FM if FM and EP(>11con) are in sensor and FM is within tolerance
	 * 3. move toward closest ES in about 2 sensor radius
	 * 4. move toward friendly sensing ES
	 * 5. move toward closest ES
	 * 6. move away from closest sensed FM or FP(<11 inf)
	 * 7. move toward friendly sensing EPS
	 * 8. move toward closest EPS
	 * 9. move toward heading
	 */
	
	//things to go toward
	public MapLocation closestES = null;
	public MapLocation closestEPS = null;
	public MapLocation FsensingES = null;
	public MapLocation FsensingEPS = null;
	public int headingIndex = -1;
	
	//things to avoid
	public MapLocation closestFM = null;
	public MapLocation closestFP = null;// with less than 11 influence
	public final int AVOID_DIST_TOLERANCE = 17; //17 puts FM's as far apart in sensor range
	public boolean isCloseEP;//with conviction > 11
	
	//Flag variables
	public int sensedE;
	public int bestFlag;
	public int sensedFlag;
	public int sensedFlagID;
	
	//sensed robot information
	public RobotType riType;
	public Team riTeam;
	public int riID;
	
	public Muckraker(RobotController rc) {
		super(rc);
	}
	
	public void takeTurn() throws GameActionException{
		super.takeTurn();
		
		//reset target if arrived at a stored target
		if(closestES != null && rc.canSenseLocation(closestES)){
			closestES = null;
		}
		if(closestEPS != null && rc.canSenseLocation(closestEPS)){
			closestEPS = null;
		}
		if(FsensingES != null && rc.canSenseLocation(FsensingES)) {
			FsensingES = null;
		}
		if(FsensingEPS != null && rc.canSenseLocation(FsensingEPS)) {
			FsensingEPS = null;
		}
		
		possibleFlagsToEcho.clear();
		bestFlag = 0;
		isCloseEP = false;
		sensedE = 0;
		
		
		for(RobotInfo ri : sensedRobots) {
			riType = ri.getType();
			riTeam = ri.getTeam();
			if(riTeam == team) {
				riID = ri.getID();
				switch(riType) {
				case ENLIGHTENMENT_CENTER:
					fecids.add(riID);
					break;
				case MUCKRAKER:
					closestFM = getClosest(closestFM, ri.getLocation());
					break;
				case POLITICIAN:
					if(ri.getInfluence() < 11) {
						closestFP = getClosest(closestFP, ri.getLocation());
					}
					break;
				case SLANDERER:
				default:
					break;
				}
				//read team mates flag for info
				if(rc.canGetFlag(riID)) {
					sensedFlag = rc.getFlag(riID);
					sensedFlagID = getFlagID(sensedFlag);
					if(sensedFlagID == REPORT_ES && !currentLocation.isWithinDistanceSquared(getLocationFromFlag(sensedFlag), type.sensorRadiusSquared)) {
						closestES = getClosest(closestES, getLocationFromFlag(sensedFlag));
					}else if(sensedFlagID == REPORT_EPS && !currentLocation.isWithinDistanceSquared(getLocationFromFlag(sensedFlag), type.sensorRadiusSquared)) {
						closestEPS = getClosest(closestEPS, getLocationFromFlag(sensedFlag));
					}else if(sensedFlagID == REPORT_FEC_ID) {
						fecids.add(getFlagFECID(sensedFlag));
					}
					
					if(sensedFlagID != REPORT_NEC && sensedFlagID != REPORT_EEC && sensedFlagID != REPORT_FEC_ID && getFlagNearbyES(sensedFlag)) {
						FsensingES = ri.getLocation();
					}
					if(sensedFlagID != REPORT_NEC && sensedFlagID != REPORT_EEC && sensedFlagID != REPORT_FEC_ID && (riType == RobotType.POLITICIAN || riType == RobotType.SLANDERER) && getFlagNearbyEP(sensedFlag)) {
						FsensingEPS = ri.getLocation();
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
					if(riTeam == enemyTeam && REPORT_EEC > getFlagID(bestFlag)) {
						bestFlag = createFlag(REPORT_EEC,0, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
					}else if(riTeam == Team.NEUTRAL && REPORT_NEC > getFlagID(bestFlag)){
						bestFlag = createFlag(REPORT_NEC,0, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
					}
					break;
				case MUCKRAKER:
					if(REPORT_EM > getFlagID(bestFlag)) {
						bestFlag = createFlag(REPORT_EM,0,0,ri.getLocation());
					}
					sensedE = sensedE | MASK_IS_EM;
					break;
				case POLITICIAN:
					if(REPORT_EP > getFlagID(bestFlag)){
						bestFlag = createFlag(REPORT_EP,0,0,ri.getLocation());
					}
					if(ri.getConviction() > 11) {
						isCloseEP = true;
					}
					sensedE = sensedE | MASK_IS_EP;
					break;
				case SLANDERER:
					if(REPORT_ES > getFlagID(bestFlag)) {
						bestFlag = createFlag(REPORT_ES,0,0,ri.getLocation());
					}
					sensedE = sensedE | MASK_IS_ES;
					closestES = getClosest(closestES, ri.getLocation());
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
				if(sensedFlagID == REPORT_ES && !currentLocation.isWithinDistanceSquared(getLocationFromFlag(sensedFlag), type.sensorRadiusSquared)) {
					closestES = getClosest(closestES, getLocationFromFlag(sensedFlag));
				}else if(sensedFlagID == REPORT_EPS && !currentLocation.isWithinDistanceSquared(getLocationFromFlag(sensedFlag), type.sensorRadiusSquared)) {
					closestEPS = getClosest(closestEPS, getLocationFromFlag(sensedFlag));
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
	        		bestFlag = createFlag(REPORT_FEC_ID,0,riID);
	        		break;
	        	}
	        	i++;
	        }
		}
		
		//add personal info to bestFlag
		//set as not slanderer
		bestFlag = bestFlag & MASK_PERSONAL_EC;
		sensedFlagID = getFlagID(bestFlag);
		//set sensing EM,EP,ES bits if unit report
		if(sensedFlagID != REPORT_NEC && sensedFlagID != REPORT_EEC && sensedFlagID != REPORT_FEC_ID) {
			bestFlag = bestFlag | sensedE;
		}
		
		//set flag
		if(rc.canSetFlag(bestFlag)) {
			rc.setFlag(bestFlag);
		}
		
		
		if(closestFM != null && !currentLocation.isWithinDistanceSquared(closestFM, AVOID_DIST_TOLERANCE)){
			closestFM = null;
		}
		if(closestFP != null && !currentLocation.isWithinDistanceSquared(closestFP, AVOID_DIST_TOLERANCE)){
			closestFP = null;
		}
		
		//expose or move
		if(rc.isReady()) {
			Direction bestDirection = null;
			
			//1. expose ES with highest influence
			if(closestES != null && rc.canExpose(closestES)) {
				RobotInfo[] enemiesInActionRadius = rc.senseNearbyRobots(type.actionRadiusSquared, enemyTeam);
				RobotInfo bestTarget = null;
				for(RobotInfo ri : enemiesInActionRadius) {
					if(ri.getType() == RobotType.SLANDERER) {
						if(bestTarget == null) {
							bestTarget = ri;
						}else if(bestTarget.getInfluence() < ri.getInfluence()) {
							bestTarget = ri;
						}
					}
				}
				if(rc.canExpose(bestTarget.getLocation())) {
					rc.expose(bestTarget.getLocation());
				}
			}
			
			//2. move away from FM if FM and EP(>11con) are in sensor and FM is within tolerance
			
			else if(isCloseEP && closestFM != null) {
				bestDirection = getBestValidDirection(closestFM.directionTo(currentLocation));
				headingIndex = -1;
			}
			
			//3. move toward closest ES in about 2 sensor radius 
			
			else if(closestES != null && currentLocation.isWithinDistanceSquared(closestES, 100)) {
				bestDirection = getBestValidDirection(closestES);
				headingIndex = -1;
			}
			
			//4. move toward friendly sensing ES
			else if(FsensingES != null) {
				bestDirection = getBestValidDirection(FsensingES);
				headingIndex = -1;
			}
			
			//5. move toward closest ES
			else if(closestES != null) {
				bestDirection = getBestValidDirection(closestES);
				headingIndex = -1;
			}
			
			//6. move away from closest sensed FM or FP(<11 inf)
			else if(closestFM != null) {
				bestDirection = getBestValidDirection(closestFM.directionTo(currentLocation));
				headingIndex = -1;
			}else if(closestFP != null) {
				bestDirection = getBestValidDirection(closestFP.directionTo(currentLocation));
				headingIndex = -1;
			}
			
			//7. move toward friendly sensing EPS
			else if(FsensingEPS != null) {
				bestDirection = getBestValidDirection(FsensingEPS);
				headingIndex = -1;
			}
			
			//8. move toward closest EPS
			else if(closestEPS != null) {
				bestDirection = getBestValidDirection(closestEPS);
				headingIndex = -1;
			}
			
			//9. move toward heading
			
			else if(headingIndex == -1){
				headingIndex = (int)(Math.random()*directions.length);
				bestDirection = directions[headingIndex];
			}else {
				bestDirection = directions[headingIndex];
			}
			
			if(bestDirection != null && rc.canMove(bestDirection)) {
				rc.move(bestDirection);
			}else {
				closestES = null;
				closestEPS = null;
				FsensingES = null;
				FsensingEPS = null;
				closestFM = null;
				closestFP = null;
				headingIndex = -1;
			}
		}
		
			
	}
	
}