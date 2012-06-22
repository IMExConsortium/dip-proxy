package edu.ucla.mbi.monitor;

/* #=======================================================================
   # $Id::                                                                $
   # Version: $Rev::                                                      $
   #=======================================================================
   #
   # Agent interface:
   #
   #
   #
   #==================================================================== */

import edu.ucla.mbi.server.WSContext;

public interface Agent extends Runnable {

    public void setContext( WSContext context );

}
