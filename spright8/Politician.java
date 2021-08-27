package spright8;

import java.util.Iterator;

import battlecode.common.*;


public class Politician extends Robot{
	
	/*
	 *New movement priorities:
	 *	Big Politician
	 * 1. move toward closest NEC
	 * 2. move toward closest EEC
	 * 3. move away from closest FP
	 * 4. move toward closest E (EP, then EM, then ES)
	 * 5. heading
	 * 
	 *  Small Politician
	 * 1. move toward closest FS > 8 away
	 * 2. move away from closest FP in action radius
	 * 3. move toward EM
	 * 4. move away from FEC
	 * 5. move toward EEC
	 * 6. heading
	 */
	
	public final int MAX_SMALL = 25;
	public final double WASTE_THRESHOLD = 0.3;
	
	public MapLocation closestNEC = null;
	public MapLocation closestEEC = null;
	public MapLocation closestFP = null;
	public final int FP_DIST_TOLERANCE = type.actionRadiusSquared;
	public MapLocation closestFM = null;
	public MapLocation closestEP = null;
	public MapLocation closestEM = null;
	public MapLocation closestES = null;
	public int headingIndex = -1;
	
	public MapLocation closestFEC = null;
	public MapLocation closestFS = null;
	public boolean isCloseFS;
	//FP,EM,EEC,heading
	
	
	//Flag variables
	public int sensedE;
	public int bestFlag;
	public int sensedFlag;
	public int sensedFlagID;
	
	//sensed robot information
	public RobotType riType;
	public Team riTeam;
	public int riID;
	
	public int currentConviction;
	public double currentEmpowerFactor;
	Direction bestDirection = null;
	public int checkedInSense;
	public final int MAX_CHECK_IN_SENSE = 28;
	
	public RobotInfo[] inActionRadius;
	public int wastedConviction;
	public int robotsDestroyed;
	public int perRobotConviction;
	public int valueOfDestroyed;
	
	public Politician(RobotController rc) {
		super(rc);
	}
	
	public Politician(RobotController rc, Robot r) {
		super(rc, r);
	}
	
	public void takeTurn() throws GameActionException{
		super.takeTurn();
		currentConviction = rc.getConviction();
		currentEmpowerFactor = rc.getEmpowerFactor(team, 0);
		
		if(currentConviction > MAX_SMALL) {
			takeTurnBig();
		}else if(currentConviction > GameConstants.EMPOWER_TAX){
			takeTurnSmall();
		}else {
			takeTurnTiny();
		}
			
	}
	
	public void takeTurnTiny() throws GameActionException{
		
		//reset target if arrived at a stored target
		closestFP = null;
		closestFM = null;
		closestFEC = null;
		
		possibleFlagsToEcho.clear();
		bestFlag = 0;
		sensedE = 0;
		checkedInSense = 0;
		isCloseFS = false;
		
		for(RobotInfo ri : sensedRobots) {
			checkedInSense++;
			if(checkedInSense > MAX_CHECK_IN_SENSE) {
				break;
			}
			riType = ri.getType();
			riTeam = ri.getTeam();
			if(riTeam == team) {
				riID = ri.getID();
				switch(riType) {
				case ENLIGHTENMENT_CENTER:
					fecids.add(riID);
					closestFEC = ri.getLocation();
					break;
				case MUCKRAKER:
					closestFM = getClosest(closestFM, ri.getLocation());
					break;
				case POLITICIAN:
					if(rc.canGetFlag(riID) && !getFlagSlanderer(rc.getFlag(riID))) {
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
					if(sensedFlagID == REPORT_FEC_ID) {
						fecids.add(getFlagFECID(sensedFlag));
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
							bestFlag = createFlag(REPORT_EEC,0, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
						}
						closestEEC = getClosest(closestEEC, ri.getLocation());
					}else if(riTeam == Team.NEUTRAL) {
						if(REPORT_NEC > getFlagID(bestFlag)) {
							bestFlag = createFlag(REPORT_NEC,0, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
						}
						closestNEC = getClosest(closestNEC, ri.getLocation());
					}
					break;
				case MUCKRAKER:
					if(REPORT_EM > getFlagID(bestFlag)) {
						bestFlag = createFlag(REPORT_EM,0,0,ri.getLocation());
					}
					closestEM = getClosest(closestEM, ri.getLocation());
					sensedE = sensedE | MASK_IS_EM;
					break;
				case POLITICIAN:
					if(REPORT_EP > getFlagID(bestFlag)){
						bestFlag = createFlag(REPORT_EPS,0,0,ri.getLocation());
					}
					closestEP = getClosest(closestEP, ri.getLocation());
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
				if(sensedFlagID == REPORT_FEC_ID) {
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
		
		if(closestFP != null && !currentLocation.isWithinDistanceSquared(closestFP, 10)){
			closestFP = null;
		}
		if(closestFM != null && !currentLocation.isWithinDistanceSquared(closestFM, 10)){
			closestFM = null;
		}
		
		//move
		if(rc.isReady()) {
			//1. move away from closest FP
			
			if(closestFP != null) {
				bestDirection = getBestValidDirection(closestFP.directionTo(currentLocation));
				headingIndex = -1;
			}
			
			//2. move away from closest FM
			else if(closestFM != null) {
				bestDirection = getBestValidDirection(closestFM.directionTo(currentLocation));
				headingIndex = -1;
			}
			
			//3. move away from FEC

			else if(closestFEC != null) {
				bestDirection = getBestValidDirection(closestFEC.directionTo(currentLocation));
			}
			
			//4. heading
			else if(headingIndex == -1){
				headingIndex = (int)(Math.random()*directions.length);
				bestDirection = directions[headingIndex];
			}else {
				bestDirection = directions[headingIndex];
			}
			
			if(bestDirection != null && rc.canMove(bestDirection)) {
				rc.move(bestDirection);
			}else {
				closestNEC = null;
				closestEEC = null;
				closestFP = null;
				closestEP = null;
				closestEM = null;
				closestES = null;
				headingIndex = -1;
			}
		}
		
	}
	
	
	public void takeTurnSmall() throws GameActionException{
		
		//reset target if arrived at a stored target
		if(closestEEC != null && rc.canSenseLocation(closestEEC)){
			closestEEC = null;
		}
		if(closestEM != null && rc.canSenseLocation(closestEM)) {
			closestEM = null;
		}
		closestFP = null;
		closestFS = null;
		closestFEC = null;
		
		possibleFlagsToEcho.clear();
		bestFlag = 0;
		sensedE = 0;
		checkedInSense = 0;
		isCloseFS = false;
		MapLocation tempLocation;
		
		for(RobotInfo ri : sensedRobots) {
			checkedInSense++;
			if(checkedInSense > MAX_CHECK_IN_SENSE) {
				break;
			}
			riType = ri.getType();
			riTeam = ri.getTeam();
			if(riTeam == team) {
				riID = ri.getID();
				switch(riType) {
				case ENLIGHTENMENT_CENTER:
					fecids.add(riID);
					closestFEC = ri.getLocation();
					break;
				case MUCKRAKER:
					break;
				case POLITICIAN:
					if(rc.canGetFlag(riID) && !getFlagSlanderer(rc.getFlag(riID))) {
						closestFP = getClosest(closestFP, ri.getLocation());
					}else {
						closestFS = getClosest(closestFS, ri.getLocation());
						isCloseFS = true;
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
					if(sensedFlagID == REPORT_EEC) {
						tempLocation = getLocationFromFlag(sensedFlag);
						if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
							closestEEC = getClosest(closestEEC, tempLocation);
						}
					}else if(sensedFlagID == REPORT_EM) {
						tempLocation = getLocationFromFlag(sensedFlag);
						if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
							closestEM = getClosest(closestEM, tempLocation);
						}
					}else if(sensedFlagID == REPORT_FEC_ID) {
						fecids.add(getFlagFECID(sensedFlag));
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
							bestFlag = createFlag(REPORT_EEC,0, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
						}
						closestEEC = getClosest(closestEEC, ri.getLocation());
					}else if(riTeam == Team.NEUTRAL) {
						if(REPORT_NEC > getFlagID(bestFlag)) {
							bestFlag = createFlag(REPORT_NEC,0, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
						}
						closestNEC = getClosest(closestNEC, ri.getLocation());
					}
					break;
				case MUCKRAKER:
					if(REPORT_EM > getFlagID(bestFlag)) {
						bestFlag = createFlag(REPORT_EM,0,0,ri.getLocation());
					}
					closestEM = getClosest(closestEM, ri.getLocation());
					sensedE = sensedE | MASK_IS_EM;
					break;
				case POLITICIAN:
					if(REPORT_EP > getFlagID(bestFlag)){
						bestFlag = createFlag(REPORT_EPS,0,0,ri.getLocation());
					}
					closestEP = getClosest(closestEP, ri.getLocation());
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
				if(sensedFlagID == REPORT_EEC) {
					tempLocation = getLocationFromFlag(sensedFlag);
					if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
						closestEEC = getClosest(closestEEC, tempLocation);
					}
				}else if(sensedFlagID == REPORT_EM) {
					tempLocation = getLocationFromFlag(sensedFlag);
					if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
						closestEM = getClosest(closestEM, tempLocation);
					}
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
		
		if(closestFP != null && !currentLocation.isWithinDistanceSquared(closestFP, FP_DIST_TOLERANCE)){
			closestFP = null;
		}
		if(closestFS != null && currentLocation.isWithinDistanceSquared(closestFS, 8)){
			closestFS = null;
		}
		if(closestEEC != null && currentLocation.isWithinDistanceSquared(closestEEC, 100)){
			closestEEC = null;
		}
		
		//move
		if(rc.isReady()) {
			//1. move toward closest FS > 8 away
			
			if(closestFS != null) {
				bestDirection = getBestValidDirection(closestFS);
				headingIndex = -1;
			}
			
			//2. move away from closest FP in action radius
			
			else if(closestFP != null) {
				bestDirection = getBestValidDirection(closestFP.directionTo(currentLocation));
				headingIndex = -1;
			}
			
			//3. move toward EM
			else if(closestEM != null) {
				bestDirection = getBestValidDirection(closestEM);
				headingIndex = -1;
			}
			
			//4. move away from FEC

			else if(closestFEC != null) {
				bestDirection = getBestValidDirection(closestFEC.directionTo(currentLocation));
			}
			
			//5. move toward EEC
			else if(closestEEC != null) {
				bestDirection = getBestValidDirection(closestEEC.directionTo(currentLocation));
			}
			
			//6. heading
			else if(headingIndex == -1){
				headingIndex = (int)(Math.random()*directions.length);
				bestDirection = directions[headingIndex];
			}else {
				bestDirection = directions[headingIndex];
			}
			
			
			//empower
			//full action radius value evaluation
			inActionRadius = rc.senseNearbyRobots(type.actionRadiusSquared);
			int conToGive = (int)((currentConviction - GameConstants.EMPOWER_TAX)*currentEmpowerFactor);
			int conGivenToEM = 0;
			int friendsInActionRadius = 0;
			if(inActionRadius.length > 0 && conToGive > 0) {
				robotsDestroyed = 0;
				perRobotConviction = conToGive / inActionRadius.length;
				for(RobotInfo ri : inActionRadius) {
					if(ri.team == enemyTeam) {
						if(perRobotConviction > ri.getConviction()&& ri.getType() == RobotType.MUCKRAKER) {
							robotsDestroyed++;
							if(isCloseFS){
								rc.empower(type.actionRadiusSquared);
							}
						}
						if(ri.getType() == RobotType.MUCKRAKER) {
							if(ri.getConviction() < perRobotConviction) {
								conGivenToEM += ri.getConviction();
							}else {
								conGivenToEM += perRobotConviction;
							}
						}
					}else {
						friendsInActionRadius++;
					}
				}
				if((double)conGivenToEM / conToGive > 0.75 || 
						((double)conGivenToEM / conToGive > 0.5 && robotsDestroyed > 0) ||
						robotsDestroyed > 1) {
					rc.empower(type.actionRadiusSquared);
				}
			}
			
			//1 rad squared value evaluation
			inActionRadius = rc.senseNearbyRobots(1);
			conToGive = (int)((currentConviction - GameConstants.EMPOWER_TAX)*currentEmpowerFactor);
			conGivenToEM = 0;
			
			if(inActionRadius.length > 0 && conToGive > 0) {
				perRobotConviction = conToGive / inActionRadius.length;
				robotsDestroyed = 0;
				for(RobotInfo ri : inActionRadius) {
					if(ri.team == enemyTeam) {
						if(friendsInActionRadius > 6) {
							rc.empower(1);
						}
						if(perRobotConviction > ri.getConviction()&& ri.getType() == RobotType.MUCKRAKER) {
							robotsDestroyed++;
							if(isCloseFS){
								rc.empower(1);
							}
						}
						if(ri.getType() == RobotType.MUCKRAKER) {
							if(ri.getConviction() < perRobotConviction) {
								conGivenToEM += ri.getConviction();
							}else {
								conGivenToEM += perRobotConviction;
							}
						}
					}
				}
				if((double)conGivenToEM / conToGive > 0.75 || 
						((double)conGivenToEM / conToGive > 0.5 && robotsDestroyed > 0) ||
						robotsDestroyed > 1) {
					rc.empower(1);
				}
			}
			
			
			if(bestDirection != null && rc.canMove(bestDirection)) {
				rc.move(bestDirection);
			}else {
				closestNEC = null;
				closestEEC = null;
				closestFP = null;
				closestEP = null;
				closestEM = null;
				closestES = null;
				headingIndex = -1;
			}
		}
		
		
		
		
		
	}
	
	
	public void takeTurnBig() throws GameActionException{
		
		//reset target if arrived at a stored target
		if(closestNEC != null && rc.canSenseLocation(closestNEC)){
			closestNEC = null;
		}
		if(closestEEC != null && rc.canSenseLocation(closestEEC)){
			closestEEC = null;
		}
		if(closestEP != null && rc.canSenseLocation(closestEP)) {
			closestEP = null;
		}
		if(closestEM != null && rc.canSenseLocation(closestEM)) {
			closestEM = null;
		}
		if(closestES != null && rc.canSenseLocation(closestES)) {
			closestES = null;
		}
		closestFP = null;
		
		possibleFlagsToEcho.clear();
		bestFlag = 0;
		sensedE = 0;
		checkedInSense = 0;
		MapLocation tempLocation;
		
		for(RobotInfo ri : sensedRobots) {
			checkedInSense++;
			if(checkedInSense > MAX_CHECK_IN_SENSE) {
				break;
			}
			riType = ri.getType();
			riTeam = ri.getTeam();
			if(riTeam == team) {
				riID = ri.getID();
				switch(riType) {
				case ENLIGHTENMENT_CENTER:
					fecids.add(riID);
					break;
				case MUCKRAKER:
					break;
				case POLITICIAN:
					if(rc.canGetFlag(riID) && !getFlagSlanderer(rc.getFlag(riID))) {
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
					if(sensedFlagID == REPORT_NEC) {
						tempLocation = getLocationFromFlag(sensedFlag);
						if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
							closestNEC = getClosest(closestNEC, tempLocation);
						}
					}else if(sensedFlagID == REPORT_EEC) {
						tempLocation = getLocationFromFlag(sensedFlag);
						if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
							closestEEC = getClosest(closestEEC, tempLocation);
						}
					}else if(sensedFlagID == REPORT_EP || sensedFlagID == REPORT_EPS) {
						tempLocation = getLocationFromFlag(sensedFlag);
						if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
							closestEP = getClosest(closestEP, tempLocation);
						}
					}else if(sensedFlagID == REPORT_EM) {
						tempLocation = getLocationFromFlag(sensedFlag);
						if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
							closestEM = getClosest(closestEM, tempLocation);
						}
					}else if(sensedFlagID == REPORT_ES) {
						tempLocation = getLocationFromFlag(sensedFlag);
						if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
							closestES = getClosest(closestES, tempLocation);
						}
					}else if(sensedFlagID == REPORT_FEC_ID) {
						fecids.add(getFlagFECID(sensedFlag));
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
							bestFlag = createFlag(REPORT_EEC,0, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
						}
						closestEEC = getClosest(closestEEC, ri.getLocation());
					}else if(riTeam == Team.NEUTRAL) {
						if(REPORT_NEC > getFlagID(bestFlag)) {
							bestFlag = createFlag(REPORT_NEC,0, (int)Math.min(15, Math.ceil(ri.getConviction() / 50.0)), ri.getLocation());
						}
						closestNEC = getClosest(closestNEC, ri.getLocation());
					}
					break;
				case MUCKRAKER:
					if(REPORT_EM > getFlagID(bestFlag)) {
						bestFlag = createFlag(REPORT_EM,0,0,ri.getLocation());
					}
					closestEM = getClosest(closestEM, ri.getLocation());
					sensedE = sensedE | MASK_IS_EM;
					break;
				case POLITICIAN:
					if(REPORT_EP > getFlagID(bestFlag)){
						bestFlag = createFlag(REPORT_EPS,0,0,ri.getLocation());
					}
					closestEP = getClosest(closestEP, ri.getLocation());
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
				if(sensedFlagID == REPORT_NEC) {
					tempLocation = getLocationFromFlag(sensedFlag);
					if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
						closestNEC = getClosest(closestNEC, tempLocation);
					}
				}else if(sensedFlagID == REPORT_EEC) {
					tempLocation = getLocationFromFlag(sensedFlag);
					if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
						closestEEC = getClosest(closestEEC, tempLocation);
					}
				}else if(sensedFlagID == REPORT_EP || sensedFlagID == REPORT_EPS) {
					tempLocation = getLocationFromFlag(sensedFlag);
					if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
						closestEP = getClosest(closestEP, tempLocation);
					}
				}else if(sensedFlagID == REPORT_EM) {
					tempLocation = getLocationFromFlag(sensedFlag);
					if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
						closestEM = getClosest(closestEM, tempLocation);
					}
				}else if(sensedFlagID == REPORT_ES) {
					tempLocation = getLocationFromFlag(sensedFlag);
					if(!currentLocation.isWithinDistanceSquared(tempLocation, type.sensorRadiusSquared)) {
						closestES = getClosest(closestES, tempLocation);
					}
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
		
		if(closestFP != null && !currentLocation.isWithinDistanceSquared(closestFP, FP_DIST_TOLERANCE)){
			closestFP = null;
		}
		
		//move
		if(rc.isReady()) {
			//1. move toward closest NEC
			if(closestNEC != null) {
				bestDirection = getBestValidDirection(closestNEC);
				headingIndex = -1;
			}
			
			//2. move toward closest EEC
			
			else if(closestEEC != null) {
				bestDirection = getBestValidDirection(closestEEC);
				headingIndex = -1;
			}
			
			//3. move away from closest FP
			
			else if(closestFP != null) {
				bestDirection = getBestValidDirection(closestFP.directionTo(currentLocation));
				headingIndex = -1;
			}
			
			//4. move toward closest EP
			else if(closestEP != null) {
				bestDirection = getBestValidDirection(closestEP);
				headingIndex = -1;
			}
			
			//5. move toward closest EM
			else if(closestEM != null) {
				bestDirection = getBestValidDirection(closestEM);
				headingIndex = -1;
			}
			
			//6. move toward closest ES
			else if(closestES != null) {
				bestDirection = getBestValidDirection(closestES);
				headingIndex = -1;
			}
			
			//7. move toward heading
			
			else if(headingIndex == -1){
				headingIndex = (int)(Math.random()*directions.length);
				bestDirection = directions[headingIndex];
			}else {
				bestDirection = directions[headingIndex];
			}
			
			
			//empower
			if(closestNEC != null && closestNEC.isWithinDistanceSquared(currentLocation, 1)) {
				inActionRadius = rc.senseNearbyRobots(1);
				if(inActionRadius.length == 1) {
					rc.empower(1);
				}
			}
			
			if(closestEEC != null && closestEEC.isWithinDistanceSquared(currentLocation, 1)) {
				inActionRadius = rc.senseNearbyRobots(1);
				if(inActionRadius.length == 1) {
					rc.empower(1);
				}
			}
			
			//full action radius value evaluation
			inActionRadius = rc.senseNearbyRobots(type.actionRadiusSquared);
			wastedConviction = 0;
			robotsDestroyed = 0;
			int friendsInActionRadius = 0;
			if(inActionRadius.length > 0) {
				perRobotConviction = ((int)((currentConviction - GameConstants.EMPOWER_TAX)*currentEmpowerFactor)) / inActionRadius.length;
				for(RobotInfo ri : inActionRadius) {
					if(ri.team == team) {
						friendsInActionRadius++;
						wastedConviction += Math.max(0, perRobotConviction - (ri.getInfluence() - ri.getConviction()));
					}else {
						if(perRobotConviction > ri.getConviction()) {
							robotsDestroyed++;
						}
						if(ri.getType() == RobotType.MUCKRAKER) {
							wastedConviction += Math.max(0, perRobotConviction - ri.getConviction());
						}else if(ri.getType() == RobotType.POLITICIAN) {
							wastedConviction += Math.max(0, perRobotConviction - (ri.getInfluence() + ri.getConviction()));
						}
					}
				}
				
				valueOfDestroyed = (robotsDestroyed-1) *2* (int)Math.ceil(0.2 * Math.sqrt(roundNum+10));
				if((double)(10 + wastedConviction - valueOfDestroyed) / ((rc.getConviction() - GameConstants.EMPOWER_TAX)*currentEmpowerFactor) < ((-3.0/(currentEmpowerFactor+2))+1)) {
					rc.empower(type.actionRadiusSquared);
				}
			}
			
			//1 rad squared value evaluation
			inActionRadius = rc.senseNearbyRobots(1);
			wastedConviction = 0;
			robotsDestroyed = 0;
			
			if(inActionRadius.length > 0) {
				perRobotConviction = ((int)((currentConviction - GameConstants.EMPOWER_TAX)*currentEmpowerFactor)) / inActionRadius.length;
				for(RobotInfo ri : inActionRadius) {
					if(ri.team == team) {
						wastedConviction += Math.max(0, perRobotConviction - (ri.getInfluence() - ri.getConviction()));
					}else {
						if(friendsInActionRadius > 6) {
							rc.empower(1);
						}
						if(perRobotConviction > ri.getConviction()) {
							robotsDestroyed++;
						}
						if(ri.getType() == RobotType.MUCKRAKER) {
							wastedConviction += Math.max(0, perRobotConviction - ri.getConviction());
						}else if(ri.getType() == RobotType.POLITICIAN) {
							wastedConviction += Math.max(0, perRobotConviction - (ri.getInfluence() + ri.getConviction()));
						}
					}
				}
				valueOfDestroyed = (robotsDestroyed-1) *2* (int)Math.ceil(0.2 * Math.sqrt(roundNum+10));
				if((double)(10 + wastedConviction - valueOfDestroyed) / ((rc.getConviction() - GameConstants.EMPOWER_TAX)*rc.getEmpowerFactor(team,0)) < ((-3.0/(currentEmpowerFactor+2))+1)) {
					rc.empower(1);
				}
			}
			
			
			if(bestDirection != null && rc.canMove(bestDirection)) {
				rc.move(bestDirection);
			}else {
				closestNEC = null;
				closestEEC = null;
				closestFP = null;
				closestEP = null;
				closestEM = null;
				closestES = null;
				headingIndex = -1;
			}
		}
		
		
		
		
		
	}
	
}