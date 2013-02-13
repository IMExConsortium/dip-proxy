package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * RemoteNativeServices:
 *  returns a remote proxy or remote native record;
 *
 *======================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.proxy.router.*;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.server.*;

class RemoteNativeService extends Observable {

    private Log log = LogFactory.getLog( RemoteNativeService.class );

    protected String provider;
    protected RemoteServerContext rsc;
    protected Router router;

    protected RemoteNativeService( String provider, 
                                   Router router, 
                                   RemoteServerContext rsc ) {
        this.provider = provider;
        this.router = router;
        this.rsc = rsc;
    }
    
    protected RemoteNativeService() { }
    
    //--------------------------------------------------------------------------
        
    //protected RemoteServer selectNextRemoteServer( String provider,
    private  NativeServer selectNextRemoteServer( String provider,
                                                   String service,
                                                   String namespace,
                                                   String accession ) {

        if ( rsc.isRemoteProxyOn() ) {

            log.info( " selecting next proxy..." );

            // register as interested
            // ----------------------

            log.info( " adding observer..." );
            this.addObserver( router );

            log.info( "before router getNextProxyServer. " );
            return router.getNextProxyServer( provider, service, 
                                              namespace, accession );
        }

        return rsc.getNativeServer();

    }

    protected NativeRecord getNativeFromRemote ( String provider, 
                                                 String service, 
                                                 String ns, 
                                                 String ac 
                                                 ) throws ProxyFault {

        int retry = rsc.getMaxRetry();
        NativeRecord remoteRecord = null;
        ProxyFault faultOfRetry = null;
 
        log.info( "getNativeFromRemote: retry=" + retry );
  
        while ( retry > 0 && remoteRecord == null ) {    
            
            log.info( "getNativeFromRemote: before selectNextRemoteServer. " );

            NativeServer nativeServer = null;

            //if( rsc.isRemoteProxyOn() && retry > 0 ){
            if( rsc.isRemoteProxyOn() ) {
                nativeServer = router.getNextProxyServer( provider, service,
                                                          ns, ac );
            } else {
                nativeServer = rsc.getNativeServer();
            }

            log.info( " retries left=" + retry );
            retry--;
                
            try {                
                //remoteRecord = rs.getNative( provider, service, ns, ac, 
                //                             rsc.getTimeout(), retry );
                log.info( "getNativeFromRemote: before getNative. " );
                remoteRecord = nativeServer.getNative( 
                    provider, service, ns, ac, rsc.getTimeout() );
                log.info( "getNativeFromRemote: after getNative. " );
            } catch( ProxyFault fault ) {
                log.warn( "getNativeFromRemote: RemoteServer getNative() " + 
                          "fault: " + fault.getFaultInfo().getMessage()); 
                faultOfRetry = fault;      
            } catch ( Exception ex ) {
                log.warn( "getNativeFromRemote: ex=" + ex.toString() );
                faultOfRetry = FaultFactory.newInstance( Fault.UNKNOWN );
            }
            
            log.info( "getNativeFromRemote: after got remoteRecord=" + 
                      remoteRecord );
            
        }

        if( remoteRecord == null ) {
            if( faultOfRetry != null ) {
                throw faultOfRetry;
            } 
            return null;
        }

        String natXml = remoteRecord.getNativeXml();

        if( natXml == null || natXml.length() == 0 ) {            
            // remote site problem
            // NOTE: should also drop on exception remote exception ???

            // ----------------------------------
            // NOTE: temporary hiding for proxy
            // -------------------------------
            /*
            log.info( "getNative: natXml is null/zero length. " );
            this.setChanged(); // drop site from DHT

            DhtRouterMessage message =
                new DhtRouterMessage( DhtRouterMessage.DELETE,
                                      remoteRecord, nativeServer );

            this.notifyObservers( message );
            this.clearChanged();
            */
            //------------------------------------------------------
            //remoteRecord = null;
                    
            throw FaultFactory.newInstance( Fault.VALIDATION_ERROR );
        }
        

        if( remoteRecord.getExpireTime() == null ) {
            //*** remoteRecord is newly created from remote native 
            remoteRecord.resetExpireTime( remoteRecord.getCreateTime(), 
                                          rsc.getTtl() );
            
            // ---------------------------------------------------
            // NOTE: temporary hiding for proxy
            // ---------------------------------------------------
            /*
            this.setChanged(); // update site from DHT

            DhtRouterMessage message =
                new DhtRouterMessage( DhtRouterMessage.UPDATE,
                                      remoteRecord, nativeServer );

            this.notifyObservers( message );
            this.clearChanged();
            */
            // -----------------------------------------------------
        }
   
        return remoteRecord;
    }

}
