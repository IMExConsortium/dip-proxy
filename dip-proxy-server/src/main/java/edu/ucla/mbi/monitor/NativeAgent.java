package edu.ucla.mbi.monitor;

/* #=========================================================================
 # $Id::                                                                    $
 # Version: $Rev::                                                          $
 #===========================================================================
 #
 # NativeAgent: 
 #    monitors/updates native records
 #   
 #
 #======================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import edu.ucla.mbi.server.*;
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.cache.orm.*;
import edu.ucla.mbi.proxy.router.*;
import edu.ucla.mbi.proxy.ProxyFault;

public class NativeAgent implements Agent {

    private WSContext context = null;
    private String order = "expiration-first";
    
    private Map<String, Router> observerMap = new HashMap<String, Router>();

    public void addObserver( String provider, Router router ) {
        observerMap.put( provider, router );
    }

    public void notifyObserver ( String provider, Object arg ) {
        Router router = observerMap.get( provider );
        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "updating router=" + router );
        
        router.update( this, arg );
    }

    
    public void setContext( WSContext context ) {

        this.context = context;

        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "after setContext" );
    } 

    public void setOrder ( String order ) {
        this.order = order;
    }

    public void initialize() {
        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "initializing... " );
        //Set<String> providers = WSContext.getServices().keySet();

        Set<String> providers = context.getServices().keySet();

        for ( Iterator<String> ii = providers.iterator(); ii.hasNext(); ) {

            String prv = ii.next();

            RemoteServerContext rsc = context.getServerContext( prv );

            log.info( "provider=" + prv );
            observerMap.put( prv, rsc.getRouter() );
        }
    }

    public void run() {

        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "running" );

        NativeRecordDAO ndo = DipProxyDAO.getNativeRecordDAO();

        if ( ndo != null ) {

            // go over providers
            // ------------------

            Set<String> providers = context.getServices().keySet();
            //Set<String> providers = WSContext.getServices().keySet();

            for ( Iterator<String> ii = providers.iterator(); ii.hasNext(); ) {

                String prv = ii.next();

                RemoteServerContext rsc = context.getServerContext( prv );

                List<String[]> oldList = null;
                try {
                    if( order.equals( "query-first" ) ) {
                        log.info( "call queryFirst. " );
                        oldList = ndo.getQueryFirst( prv );
                    } else {
                        log.info( "call expireFirst. " );
                        oldList = ndo.getExpireFirst( prv );
                    }

                    for ( Iterator<String[]> jj = oldList.iterator(); jj
                            .hasNext(); ) {

                        String[] old = jj.next();

                        String service = old[0];
                        String ns = old[1];
                        String ac = old[2];

                        log.info( "updating: " + prv + ":" + service + ":" + ns
                                + ":" + ac );

                        try {
                            NativeRecord natRec = 
                                rsc.getNativeServer()
                                        .getNative( prv, service, ns,
                                                    ac, rsc.getTimeout() );

                            //*** check record validation
                            if( natRec.getNativeXml() == null
                                || natRec.getNativeXml().isEmpty() ) {
        
                                continue;
                            }

                            NativeRecord oldRecord =
                                    ndo.find( prv, service, ns, ac );

                            log.info( "Native rec: " + oldRecord );

                            if ( oldRecord == null ) {
                                oldRecord =
                                        new NativeRecord( prv, service, ns, ac );
                            }

                            oldRecord.setNativeXml( natRec.getNativeXml() );
                            oldRecord.resetExpireTime( Calendar.getInstance()
                                    .getTime(), rsc.getTtl() );

                            // store native record locally
                            ndo.create( oldRecord );

                            // notify dht to update
                            if( rsc.isDbCacheOn() ) {

                                DhtRouterMessage message =
                                    new DhtRouterMessage( DhtRouterMessage.UPDATE,
                                                          oldRecord, null );

                                log.info( "DhtRouterMessage: " + message );

                                this.notifyObserver( prv, message );
                            }

                        } catch ( ProxyFault fault ) {
                            log.info( "remote service (" + prv + ") " + fault );
                        }
                    }
                } catch ( Exception e ) {
                    log.info( "exception(" + prv + ") " + e );
                }
            }
        }
        log.info( "done" );
    }
}
