package edu.ucla.mbi.proxy.dht;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * DhtProxyFault:
 *
 *========================================================================= */

import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceFault;

public class DhtProxyFault extends DhtFault {

    private static final String MESSAGE = "DhtProxyFault";

    public DhtProxyFault( int code ) {
        super( MESSAGE, Fault.getServiceFault( code ) );
    }

    public DhtProxyFault( ServiceFault fault ) {
        super( MESSAGE, fault );
    }

    public static DhtFault getFault( int code ) {
        return new DhtFault( MESSAGE, Fault.getServiceFault( code ) );
    }

    public static DhtFault getFault( ServiceFault fault ) {
        return new DhtFault( MESSAGE, fault );
    }


}
