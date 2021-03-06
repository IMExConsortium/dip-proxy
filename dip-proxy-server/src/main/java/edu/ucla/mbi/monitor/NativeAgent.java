package edu.ucla.mbi.monitor;

/* #============================================================================
   # $Id::                                                                     $
   # Version: $Rev::                                                           $
   #============================================================================
   #
   # NativeAgent: 
   #    monitors/updates native records
   #   
   #
   #========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.cache.orm.*;
import edu.ucla.mbi.proxy.context.*;

public class NativeAgent implements Agent {

    private WSContext wsContext = null;
    private String order = "expiration-first";
    
    public void setWsContext( WSContext context ) {

        this.wsContext = context;

        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "after setContext" );
    } 

    public void setOrder ( String order ) {
        this.order = order;
    }

    public void initialize() {
        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "initializing... " );
    }

    public void run() {

        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "running" );

        NativeRecordDAO ndo = 
            wsContext.getDipProxyDAO().getNativeRecordDAO();

        if ( ndo != null ) {

            // go over providers
            // ------------------
            Set<String> providers = wsContext.getProviderSet();

            for ( Iterator<String> ii = providers.iterator(); ii.hasNext(); ) {

                String curProv = ii.next();
                //List<String[]> oldList = null;
                List<NativeRecord> oldList = null;

                try {
                    if( order.equals( "query-first" ) ) {
                        log.info( "call queryFirst. " );
                        oldList = ndo.getQueryFirst( curProv );
                    } else {
                        log.info( "call expireFirst. " );
                        oldList = ndo.getExpireFirst( curProv );
                    }

                    //for ( Iterator<String[]> jj = oldList.iterator(); jj
                    for ( Iterator<NativeRecord> jj = oldList.iterator(); jj
                            .hasNext(); ) {

                        NativeRecord nr = jj.next();
                        /*
                        String[] old = jj.next();

                        String service = old[0];
                        String ns = old[1];
                        String ac = old[2];
                        */
                        String service = nr.getService();
                        String ns = nr.getNs();
                        String ac = nr.getAc();   
                        log.info( "updating: " + curProv + ":" + service + ":" + ns
                                + ":" + ac );

                        try {
                            
                            NativeRecord natRec = 
                                wsContext.getNativeServer( curProv )
                                .getNativeRecord( curProv, service, ns, ac,
                                            wsContext.getTimeout( curProv ) );
                            
                            //*** check record validation
                            if( natRec.getNativeXml() == null
                                || natRec.getNativeXml().isEmpty() ) {
        
                                continue;
                            }

                            //NativeRecord oldRecord =
                            //        ndo.find( curProv, service, ns, ac );
                            NativeRecord oldRecord = nr;
    
                            log.info( "Native rec: " + oldRecord );

                            if ( oldRecord == null ) {
                                oldRecord =
                                        new NativeRecord( curProv, service, ns, ac );
                            }

                            oldRecord.setNativeXml( natRec.getNativeXml() );
                            oldRecord.resetExpireTime( Calendar.getInstance()
                                .getTime(), wsContext.getTtl( curProv ) );

                            // store native record locally
                            ndo.create( oldRecord );

                        } catch ( ServerFault fault ) {
                            log.info( "remote service (" + curProv + ") " + fault );
                        }
                    }
                } catch ( Exception e ) {
                    log.info( "exception(" + curProv + ") " + e );
                }
            }
        }
        log.info( "done" );
    }
}
