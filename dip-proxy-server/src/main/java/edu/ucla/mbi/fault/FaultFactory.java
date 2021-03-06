package edu.ucla.mbi.fault;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * FaultFactory:
 *
 *========================================================================= */

import edu.ucla.mbi.proxy.ProxyFault;

public class FaultFactory {

    private static final String MESSAGE = "ProxyFault:";
    
    public static ProxyFault newInstance( int code ) {
        ServiceFault serviceFault = new ServiceFault();
        serviceFault.setFaultCode( code );
        
        String message;
        if( Fault.fault.get(code) != null ) {
            message = Fault.fault.get( code );
        } else {
            message = Fault.fault.get( 99 );
        }
        serviceFault.setMessage( message );

        return new ProxyFault( MESSAGE, serviceFault);
    }

    public static ProxyFault newInstance( ServiceFault fault ) {
        return new ProxyFault( MESSAGE, fault );
    }


}
