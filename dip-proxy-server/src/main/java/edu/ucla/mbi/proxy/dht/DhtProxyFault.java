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
import edu.ucla.mbi.proxy.FaultFactory;

public class DhtProxyFault extends DhtFault {

    private static final String MESSAGE = "DhtProxyFault";

    public DhtProxyFault( int code ) {
        super( MESSAGE, FaultFactory.newInstance( code ).getFaultInfo() );
    }

    public DhtProxyFault( ServiceFault fault ) {
        super( MESSAGE, fault );
    }

    public static DhtFault getFault( int code ) {
        ServiceFault serviceFault = new ServiceFault();
        serviceFault.setFaultCode( code );
        return new DhtFault( MESSAGE, serviceFault );
    }

    public static DhtFault getFault( ServiceFault fault ) {
        return new DhtFault( MESSAGE, fault );
    }


}
