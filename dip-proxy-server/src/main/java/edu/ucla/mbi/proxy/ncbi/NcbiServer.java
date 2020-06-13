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

    private NativeServer nativeRestServer = null;

    private int threadRunSec = 10 ;
    private int maxThreadNum = 10;
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

        nativeRestServer = (NativeServer) context.get( "nativeRestServer" );

        threadRunSec = 
            Integer.parseInt( (String)context.get( "threadRunSec" ) );        

        maxThreadNum = Integer.parseInt((String)context.get( "maxThreadNum" ));

        wsContext = (WSContext) context.get( "wsContext" );

        if( nativeRestServer == null && wsContext == null ) {
            log.warn( "NcbiServer: initializing failed " +
                      "because nativeRestServer is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
    }

    public NativeRecord getNativeRecord( String provider, String service, 
                                         String ns, String ac, int timeout )
        throws ServerFault {
        
        log.info( "NcbiServer.getNativeRecord called" );
        log.info( "NcbiServer: NS=" + ns + " AC=" + ac + " OP=" + service );
        
        if ( !service.equals( "nlm" ) ) {
            
            return nativeRestServer.getNativeRecord( provider, service,
                                                     ns, ac, timeout );
            
        } else { 

            NativeRecord record = null;
            String ncbi_nlmid = "";
       
            log.info( "threadCount=" + wsContext.getThreadCount() ); 
            try {
                log.info( "NcbiGetJournal.easarch  ac=" + ac );
                ncbi_nlmid = ((NcbiGetJournal) context
                              .get("ncbiGetJournal")).esearch( ac );

		log.info( "ncbi_nlmid=" + ncbi_nlmid );
               
            } catch ( ServerFault sf ) {
		log.info( " NcbiServer: NcbiGetJournal.getNativeRecord: esearch faulted " + ac );
                
                /*
                if( sf.getFaultCode() == Fault.REMOTE_FAULT 
                    && wsContext.isDbCacheOn( provider )
                    && wsContext.getThreadCount() < maxThreadNum ) {

                    NcbiReFetchThread thread =
                        new NcbiReFetchThread( ns, ac, timeout,
                                               threadRunSec, 
                                               (NcbiGetJournal)context
                                               .get("ncbiGetJournal"),
                                               wsContext );

                    thread.start_verify();
		    log.info( "NcbiReFetchThread started"); 
                }
                */
                log.info( "NcbiServer.getNativeRecord fault(1)");
                throw sf;
            } 

            if( ncbi_nlmid.equals( "" ) ) {
                log.info( "NcbiServer.getNativeRecord fault(2)");
                throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
            }

            try {
                log.info( "before efetch with ac=" + ncbi_nlmid );
                record = ( (NcbiGetJournal)context.get("ncbiGetJournal") )
                    .efetch( ns, ncbi_nlmid, timeout );

            } catch ( ServerFault sf ) {

                /* 
                if( sf.getFaultCode() == Fault.REMOTE_FAULT 
                    && wsContext.isDbCacheOn( provider ) 
                    && wsContext.getThreadCount() < maxThreadNum ) {

                    NcbiReFetchThread thread =
                         new NcbiReFetchThread( ns, ncbi_nlmid, timeout,
                                                threadRunSec,
                                                (NcbiGetJournal)context
                                                .get("ncbiGetJournal"),
                                                wsContext );
                    thread.start_no_verify();
                }
                */
                log.info( "NcbiServer.getNativeRecord fault(3)");
                throw sf;
            }
            log.info( "NcbiServer.getNativeRecord:DONE" );
            return record;
        }
    }       
}
