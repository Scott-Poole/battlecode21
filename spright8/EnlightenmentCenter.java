package spright8;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import battlecode.common.*;

public class EnlightenmentCenter extends Robot{
	
	HashSet<Integer> fids;
	
	public final int MAX_UNITS_STORED = 50;
	
	public int[] idealSlandererInfluence = {0,21,41,63,85,107,130,154,178,203,228,255,282,310,339,368,399,431,463,497,532,568,605,643,683,724,766,810,855,902,949,999};

	public int closestNEC = 0;
	public int closestEEC = 0;
	public MapLocation closestEM = null;
	
	//Flag variables
	public int sensedE;
	public int bestFlag;
	public int bestFlagID;
	public int sensedFlag;
	public int sensedFlagID;
	
	//sensed robot information
	public RobotType riType;
	public Team riTeam;
	public int riID;
	
	public int roundsClean = ECHO_WIPE_RATE;
	
	/*
	 * 1 - pol (17-25)
	 * 2 - muck (1)
	 * 3 - pol (big enough to kill NEC,EEC)
	 * 4 - sland (one minus from optimal) 
	 */
	public int[] buildOrder;
	public int[] buildOrderNoSland = {1,2,3};
	public int[] buildOrderSland = {4,2,2,1,3};
	public int toBuildIndex = 0;
	
	public boolean lostLastVote = true;
	public int votesLastRound = 0;
	public int lastBid = 2;
	
	public EnlightenmentCenter(RobotController rc) {
		super(rc);
		fids = new HashSet<Integer>();
		
	}
	
	public void takeTurn() throws GameActionException{
		super.takeTurn();
		closestEEC = 0;
		closestNEC = 0;
		closestEM = null;
		Direction temp;
		int index;
	    Random random = new Random();
	    for (int i = directions.length - 1; i > 0; i--)
	    {
	        index = random.nextInt(i + 1);
	        temp = directions[index];
	        directions[index] = directions[i];
	        directions[i] = temp;
	    }
		
		possibleFlagsToEcho.clear();
		bestFlag = 0;
		sensedE = 0;
		
		if(rc.getTeamVotes() < GameConstants.GAME_MAX_NUMBER_OF_ROUNDS / 2 + 1) {
			lostLastVote = votesLastRound == rc.getTeamVotes();
			votesLastRound = rc.getTeamVotes();
			int curInf = rc.getInfluence();
			if(curInf > 2000) {
				if(lostLastVote && lastBid*2 < curInf/2) {
					lastBid *= 2;
				}
				//check -1 to bid to see if they are changing vote
				if(!lostLastVote && lastBid > 2) {
					lastBid--;
				}
				if(rc.canBid(lastBid)) {
					rc.bid(lastBid);
				}
			}else if(curInf > 350) {
				if(lostLastVote && lastBid+1 < curInf/2) {
					lastBid++;
				}
				//check -1 to bid to see if they are changing vote
				if(!lostLastVote && lastBid > 2) {
					lastBid--;
				}
				if(rc.canBid(lastBid)) {
					rc.bid(lastBid);
				}
			}
			
			
			
		}
		//vote done
		
		
		for(RobotInfo ri : sensedRobots) {
			riType = ri.getType();
			riTeam = ri.getTeam();
			if(riTeam == team) {
				if(fids.size() < MAX_UNITS_STORED && (riType == RobotType.MUCKRAKER || riType == RobotType.POLITICIAN)) {
					fids.add(ri.getID());
				}
			}else {
				switch(riType) {
				case ENLIGHTENMENT_CENTER:
					break;
				case MUCKRAKER:
					closestEM = getClosest(closestEM, ri.getLocation());
					sensedE = sensedE | MASK_IS_EM;
					break;
				case POLITICIAN:
					sensedE = sensedE | MASK_IS_EP;
					break;
				case SLANDERER:
					sensedE = sensedE | MASK_IS_ES;
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
				bestFlagID = getFlagID(bestFlag);
				if(sensedFlagID == REPORT_EEC || sensedFlagID == REPORT_NEC) {
					possibleFlagsToEcho.add(sensedFlag & MASK_PERSONAL_EC);
				}else if(sensedFlagID != REPORT_FEC_ID){
					possibleFlagsToEcho.add(sensedFlag & MASK_PERSONAL);
				}
	  		}else {
	  			iter.remove();
	  		}
        }
		for(Iterator<Integer> iter = fids.iterator(); iter.hasNext();){
			riID = iter.next();
        	if(rc.canGetFlag(riID)) {
        		sensedFlag = rc.getFlag(riID);
				sensedFlagID = getFlagID(sensedFlag);
				bestFlagID = getFlagID(bestFlag);
				if(sensedFlagID == REPORT_EEC || sensedFlagID == REPORT_NEC) {
					possibleFlagsToEcho.add(sensedFlag & MASK_PERSONAL_EC);
				}else if(sensedFlagID != REPORT_FEC_ID){
					possibleFlagsToEcho.add(sensedFlag & MASK_PERSONAL);
				}
	  		}else {
	  			iter.remove();
	  		}
        }
		
		for(Iterator<Integer> iter = possibleFlagsToEcho.iterator(); iter.hasNext();){
        	sensedFlag = iter.next();
        	sensedFlagID = getFlagID(sensedFlag);
        	bestFlagID = getFlagID(bestFlag);
        	
        	if(sensedFlagID == REPORT_NEC) {
				closestNEC = getClosest(closestNEC, sensedFlag);
			}else if(sensedFlagID == REPORT_EEC) {
				closestEEC = getClosest(closestEEC, sensedFlag);
			}else if(sensedFlagID == REPORT_EM) {
				closestEM = getClosest(closestEM, getLocationFromFlag(sensedFlag));
			}else if(REPORT_FEC_ID == sensedFlagID) {
    			fecids.add(getFlagFECID(sensedFlag));
    		}
        	
        	if(sensedFlagID > bestFlagID) {
        		bestFlag = sensedFlag;
        	}else if(sensedFlagID == bestFlagID && (bestFlagID == REPORT_NEC || bestFlagID == REPORT_EEC)) {
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
		
		//build
		if((closestEM != null && currentLocation.isWithinDistanceSquared(closestEM, 400)) || (closestEEC != 0 && currentLocation.isWithinDistanceSquared(getLocationFromFlag(closestEEC), 400))) {
			roundsClean = 0;
		}else {
			roundsClean++;
		}
		
		while(rc.isReady() && rc.getInfluence() > 1) {
			//set build order
			if(roundsClean < ECHO_WIPE_RATE) {
				//build normal
				buildOrder = buildOrderNoSland;
				if(toBuildIndex >= buildOrder.length) {
					toBuildIndex = 0;
				}
			}else {
				//build slands
				buildOrder = buildOrderSland;
				if(toBuildIndex >= buildOrder.length) {
					toBuildIndex = 0;
				}
			}
			RobotType toBuild = RobotType.MUCKRAKER;
			int toBuildInfluence = 0;
			switch(buildOrder[toBuildIndex]) {
			case 1:
				toBuild = RobotType.POLITICIAN;
				if(rc.getInfluence() > 25) {
					toBuildInfluence = 17 + (int)(Math.random()*9);
				}
				break;
			case 2:
				toBuild = RobotType.MUCKRAKER;
				if(rc.getInfluence() > 1) {
					toBuildInfluence = 1;
				}
				break;
			case 3:
				toBuild = RobotType.POLITICIAN;
				if(closestNEC != 0 && rc.getInfluence() > 50 * getFlagConviction(closestNEC) + GameConstants.EMPOWER_TAX+1) {
					toBuildInfluence = 50 * getFlagConviction(closestNEC)  + GameConstants.EMPOWER_TAX+1;
				}else if(closestEEC != 0 && rc.getInfluence() > 50 * getFlagConviction(closestEEC) + GameConstants.EMPOWER_TAX+1) {
					toBuildInfluence = 50 * getFlagConviction(closestEEC) + GameConstants.EMPOWER_TAX+1;
				}
				break;
			case 4:
				toBuild = RobotType.SLANDERER;
				for(int i = idealSlandererInfluence.length - 1; i > 0; i--) {
					if(rc.getInfluence() > idealSlandererInfluence[i]) {
						toBuildInfluence = idealSlandererInfluence[i-1];
						break;
					}
				}
				break;
			}
			if(toBuildInfluence > 0) {
				for (Direction dir : directions) {
		            if (rc.canBuildRobot(toBuild, dir, toBuildInfluence)) {
		                rc.buildRobot(toBuild, dir, toBuildInfluence);
		                break;
		            }
		        }
				//surrounded by units
				if(rc.isReady()) {
					toBuildIndex++;
					break;
				}
			}
			
			toBuildIndex++;
		}
       
	}

}