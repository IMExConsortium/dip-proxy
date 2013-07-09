package edu.ucla.mbi.proxy.prolinks;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-prox$
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProlinksServer:
 *    services provided by Prolinks web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.proxy.*;

import java.util.*;

public class ProlinksServer implements NativeServer{

    private Log log = LogFactory.getLog( ProlinksServer.class );
    private NativeRestServer nativeRestServer = null;
    private Map<String,Object> context = null;

    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }

    public void initialize() throws ServerFault {
        if(  context == null ) {
            log.warn( "ProlinksServer: initializing failed " +
                      "because context is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        nativeRestServer = (NativeRestServer)context.get( "nativeRestServer" );

        if( nativeRestServer == null ) {
            log.warn( "ProlinksServer: initializing failed " +
                      "because nativeRestServer is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
    }

    public NativeRecord getNative( String provider, String service,
                                   String ns, String ac, int timeout  
                                   ) throws ServerFault {
        
        return nativeRestServer.getNativeRecord( provider, service, 
                                                 ns, ac, timeout );
    }
}
