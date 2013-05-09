package edu.ucla.mbi.fault;

/*===========================================================================
 * $HeadURL:: https://imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-prox#$
 * $Id:: FaultFactory.java 2424 2012-06-21 01:14:00Z wyu                    $
 * Version: $Rev:: 2424                                                     $
 *===========================================================================
 *
 * FaultFactory:
 *
 *========================================================================= */

//import edu.ucla.mbi.proxy.ProxyFault;

public class ServerFaultFactory {

    private static final String MESSAGE = "ServerFault:";
    
    /*

    public static ProxyServerFault newInstance( int code ) {
        ProxyServerFault serverFault = new ProxyServerFault();
        serverFault.setFaultCode( code );
        
        String message;
        if( ProxyServerFault.fault.get(code) != null ) {
            message = ProxyServerFault.fault.get( code );
        } else {
            message = ProxyServerFault.fault.get( 99 );
        }
        serverFault.setMessage( message );

        return new ProxyFault( MESSAGE, serviceFault);
    }
    */

    public static ServerFault newInstance( Fault fault ) {
        return new ServerFault( MESSAGE, 99 );
    }

    public static ServerFault newInstance( int code ) {
        return new ServerFault( MESSAGE, code );
    }

   
    
    public static ServerFault newInstance() {
        return new ServerFault();
    }

}
