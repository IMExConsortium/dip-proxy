package edu.ucla.mbi.proxy.ncbi;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/main$
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NCBIServer:
 *    services provided by NCBI web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.fault.*;

import java.util.Map;

public class NcbiServer implements NativeServer {
    
    private Log log = LogFactory.getLog( NcbiServer.class );
     
    private Map<String,Object> context = null;

    //private NativeRestServer nativeRestServer = null;

    private NativeServer nativeRestServer = null;

    private int threadRunMinutes = 10 ;
    private WSContext wsContext = null;

    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }

    public void initialize() throws ServerFault {
        if( context == null ) {
            log.warn( "NcbiServer: initializing failed " +
                      "because context is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        //nativeRestServer = (NativeRestServer) context.get( "nativeRestServer" );

        nativeRestServer = (NativeServer) context.get( "nativeRestServer" );

        threadRunMinutes = 
            Integer.parseInt( (String)context.get( "threadRunMinutes" ) );        
        wsContext = (WSContext) context.get( "wsContext" );

        if( nativeRestServer == null && wsContext == null ) {
            log.warn( "NcbiServer: initializing failed " +
                      "because nativeRestServer is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
    }

    public NativeRecord getNativeRecord( String provider, String service, String ns,
                                   String ac, int timeout ) throws ServerFault {

        log.info( "NcbiServer: NS=" + ns + " AC=" + ac + " OP=" + service );

        //----------------------------------------------------------------------
        if ( !service.equals( "nlm" ) ) {
            return nativeRestServer.getNativeRecord( provider, service, 
                                                     ns, ac, timeout );
        } else { 
            boolean isRetry = false;
            String retryOn = (String)context.get( "isRetry" );

            if( retryOn != null
                && ( retryOn.equals( "true" )
                     || retryOn.equalsIgnoreCase( "on" )
                     || retryOn.equalsIgnoreCase( "yes" ) ) ) {

                isRetry = true;
            }

            NativeRecord record = null;
            String ncbi_nlmid = "";
        
            try {
                ncbi_nlmid = ((NcbiGetJournal)context.get("ncbiGetJournal"))
                    .esearch( ns, ac, timeout, threadRunMinutes, isRetry );

            } catch ( RuntimeException e ) { 
                if( e.getMessage().equals( "NO_RECORD" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                } else {
                    throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                }
            }

            if( ncbi_nlmid.equals( "" ) ) {
                throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
            }

            try {
                record = ((NcbiGetJournal)context.get("ncbiGetJournal"))
                    .efetch( ns, ac, timeout, threadRunMinutes, isRetry );
            } catch ( RuntimeException e ) {
                if( e.getMessage().equals( "NO_RECORD" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                } else {
                    throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                }
            }
            
            return record;
        }
    }       
}
