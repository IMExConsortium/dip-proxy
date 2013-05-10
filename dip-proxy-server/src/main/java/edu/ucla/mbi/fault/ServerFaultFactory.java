package edu.ucla.mbi.fault;
        
/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * ServerFaultFactory:
 *
 *========================================================================= */


import edu.ucla.mbi.fault.Fault;

public class ServerFaultFactory {

    public static ServerFault newInstance( int code ) {
        
        if( Fault.getMessage( code ) != null ){
            return new ServerFault( Fault.getMessage( code ), code );
        }
        
        return new ServerFault(  Fault.getMessage( Fault.UNKNOWN ),
                                 Fault.UNKNOWN );
    }
}
