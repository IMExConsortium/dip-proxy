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

import edu.ucla.mbi.proxy.context.WSContext;

public interface Agent extends Runnable {

    public void setWsContext( WSContext context );

}
