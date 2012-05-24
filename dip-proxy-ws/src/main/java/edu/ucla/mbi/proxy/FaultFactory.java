package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * FaultFactory:
 *
 *========================================================================= */

import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceFault;

public class FaultFactory {

    private static final String MESSAGE = "ProxyFault";
    
    public static ProxyFault newInstance( int code ) {
        return new ProxyFault( MESSAGE, Fault.getServiceFault( code ) );
    }

    public static ProxyFault newInstance( ServiceFault fault ) {
        return new ProxyFault( MESSAGE, fault );
    }


}
