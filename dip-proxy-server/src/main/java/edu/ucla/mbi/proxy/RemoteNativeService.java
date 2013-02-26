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

class RemoteNativeService { // extends Observable {

    private Log log = LogFactory.getLog( RemoteNativeService.class );

    protected String provider;
    protected RemoteServerContext rsc;
    protected Router router;

    protected RemoteNativeService( String provider, 
                                   Router router, 
                                   RemoteServerContext rsc ) {
        this.provider = provider;
        this.router = router;

        log.info( " adding observer..." );
        this.addObserver( router );
        
        this.rsc = rsc;

    }
    
    protected RemoteNativeService() { }
    
    //--------------------------------------------------------------------------
        
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
        ProxyFault retryFault = null;
        
        while ( retry > 0 && remoteRecord == null ) {    
            
            NativeServer nativeServer = null;                
            retry--;

            log.info( "getNativeFromRemote: retry left=" + retry );
            log.info( "getNativeFromRemote: before selectNextRemoteServer. " );    

            if( rsc.isRemoteProxyOn() && retry > 0 ) {
                
                nativeServer = router.getNextProxyServer( provider, service,
                                                          ns, ac );
                log.info( "getNativeFromRemote: nativeServer came from proxy. " );
            } else {
                    
                //*** last retry or no proxy 
                nativeServer = rsc.getNativeServer();
                log.info( "getNativeFromRemote: nativeServer came from native. " );
                
            }
                
            try {                
                remoteRecord = nativeServer.getNative( 
                    provider, service, ns, ac, rsc.getTimeout() );

            } catch( ProxyFault fault ) {
                log.warn( "getNativeFromRemote: RemoteServer getNative() " + 
                          "fault: " + fault.getFaultInfo().getMessage()); 
                retryFault = fault;      
            } catch ( Exception ex ) {
                log.warn( "getNativeFromRemote: ex=" + ex.toString() );
                retryFault = FaultFactory.newInstance( Fault.UNKNOWN );
            }
            
            log.info( "getNativeFromRemote: after got remoteRecord=" + 
                      remoteRecord );
          
            if( remoteRecord != null && !isRecordValid( remoteRecord ) ) {
                remoteRecord = null;
                retryFault = FaultFactory.newInstance( Fault.VALIDATION_ERROR );
            }
            
        }

        

        if( remoteRecord == null ) {
            if( retryFault != null ) {
                throw retryFault;
            } 
            return null;
         }

        log.info( "valid record="+ remoteRecord);

        // valid record when here
        //-----------------------
        
        
        //  remote site problem
        //  NOTE: should also drop on exception remote exception ???
               
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
                    
        //  throw FaultFactory.newInstance( Fault.VALIDATION_ERROR );
        // }
        
        if( remoteRecord.getExpireTime() == null ) {
            
            //*** remoteRecord is newly created from remote native 
            
            remoteRecord.resetExpireTime( remoteRecord.getCreateTime(), 
                                          rsc.getTtl() );
        } else {
            //*** remoteRecord got from remote proxy
            remoteRecord.resetExpireTime( remoteRecord.getQueryTime(),
                                          rsc.getTtl() );
        } 
         
        if( rsc.isDbCacheOn() ) {

            DhtRouterMessage message =
                new DhtRouterMessage( DhtRouterMessage.UPDATE,
                                      remoteRecord, null );

            log.info( "DhtRouterMessage: " + message );
        
            this.notifyObservers( message );
        }

        return remoteRecord;
    }

    private boolean isRecordValid( NativeRecord record ) {

        if( record.getNativeXml() == null 
            || record.getNativeXml().length() == 0 ) {

            return false;
        }
        return true;
    }
    
    private List<Router> observerList = new ArrayList<Router>();
    
    public void addObserver( Router router ){
        observerList.add( router );
    }


    public void notifyObservers( Object arg){

        for(Iterator<Router> io = observerList.iterator(); io.hasNext(); ){
            
            Router r = io.next();

            log.info("updating router="+ r);
            r.update( this, arg );

        }

    }
   
}
