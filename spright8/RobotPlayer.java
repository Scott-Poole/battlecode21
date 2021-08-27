package spright8;
import battlecode.common.*;

public strictfp class RobotPlayer {
    
	public static void run(RobotController rc) throws GameActionException {

        Robot me = null;
        switch (rc.getType()) {
	        case ENLIGHTENMENT_CENTER: 
	        	me = new EnlightenmentCenter(rc);
	        	break;
	        case POLITICIAN:
	        	me = new Politician(rc);
	        	break;
	        case SLANDERER:
	        	me = new Slanderer(rc);
	        	break;
	        case MUCKRAKER:
	        	me = new Muckraker(rc);
	        	break;
	    }
        
        while (true) {
            try {
            	if(me.type != rc.getType()) {
            		System.out.println("identity switch");
            		me = new Politician(rc, me);
            	}
                me.takeTurn();
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    
}
