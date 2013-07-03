package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * RemoteNativeServices:
 *  returns a remote proxy or remote native record;
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.proxy.router.*;
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.fault.*;

class RemoteNativeService { 

    private Log log = LogFactory.getLog( RemoteNativeService.class );

    private WSContext wsContext;

    private List observerList = new ArrayList();

    public RemoteNativeService() { }
    
    public void setWsContext( WSContext context ) {
        this.wsContext = context;
    }

    //--------------------------------------------------------------------------
        
    private  NativeServer selectNextRemoteServer( String provider,
                                                  String service,
                                                  String namespace,
                                                  String accession ) {

        if ( wsContext.isRemoteProxyOn( provider ) ) {
            
            log.info( " selecting next proxy..." );

            // register as interested
            // ----------------------

            log.info( " adding observer..." );
            
            //this.addObserver( wsContext.getRouter( provider ) );

            this.addObserver( wsContext  );

            log.info( "before router getNextProxyServer. " );

            //return wsContext.getRouter( provider ).getNextProxyServer( 
            //    provider, service, namespace, accession );

            return wsContext.getNextProxyServer( provider, service, 
                                                 namespace, accession );
        }

        return wsContext.getNativeServer( provider );
    }

    public NativeRecord getNativeFromRemote ( String provider, 
                                              String service, 
                                              String ns, 
                                              String ac 
                                              ) throws ServerFault {

        int retry = wsContext.getMaxRetry( provider );
        NativeRecord remoteRecord = null;
        ServerFault retryFault = null;
        
        while ( retry > 0 && remoteRecord == null ) {    

            boolean deleteFlag = false;
            
            NativeServer nativeServer = null;                
            retry--;

            if( retryFault != null ) {
                retryFault = null;
            }

            log.info( "getNativeFromRemote: retry left=" + retry );
            log.info( "getNativeFromRemote: before selectNextRemoteServer. " );    

            
            if( wsContext.isRemoteProxyOn( provider ) && retry > 0 ) {
               
                //if( !observerList.contains( wsContext.getRouter( provider ) ) ) {
                //    this.addObserver( wsContext.getRouter( provider ) );
                //} 

                if( !observerList.contains( wsContext ) ) {
                    this.addObserver( wsContext );
                } 

                //nativeServer = wsContext.getRouter( provider )            //XXXXXXXXXX
                //    .getNextProxyServer( provider, service, ns, ac );

                nativeServer = wsContext.getNextProxyServer( provider, service, ns, ac );

                log.info( "getNativeFromRemote: nativeServer came from proxy. " );
            } else {
                    
                //*** last retry or no proxy 
                nativeServer = wsContext.getNativeServer( provider );
                log.info( "getNativeFromRemote: nativeServer came from native. " );
            }

            try {                                                         //XXXXXXXX
                remoteRecord = nativeServer.getNative( provider, service,
                    ns, ac, wsContext.getTimeout( provider ) );

            } catch( ServerFault fault ) {
                log.warn( "getNativeFromRemote: RemoteServer getNative() " + 
                          "fault: " + fault.getMessage()); 
                retryFault = fault;
                deleteFlag = true;
                
            } catch ( Exception ex ) {
                log.warn( "getNativeFromRemote: ex=" + ex.toString() );
                deleteFlag = true;
                retryFault = ServerFaultFactory.newInstance( Fault.UNKNOWN );
            }
            
            log.info( "getNativeFromRemote: after got remoteRecord=" + 
                      remoteRecord );
          
            if( remoteRecord != null && !isRecordValid( remoteRecord ) ) {

                deleteFlag = true;
                retryFault = ServerFaultFactory.newInstance( Fault.VALIDATION_ERROR );
                remoteRecord = null;
            }
            
            if( deleteFlag && nativeServer instanceof RemoteProxyServer ) {

                NativeRecord faultyRecord = new NativeRecord( provider, service, 
                                                              ns, ac);
                faultyRecord.resetExpireTime( wsContext.getTtl( provider ) );  
     
                DhtRouterMessage message =
                    new DhtRouterMessage( DhtRouterMessage.DELETE,
                                          faultyRecord, nativeServer );
                
                this.notifyObservers( provider, message );
                log.info( "getNativeFromRemote: delete a invalid record " +
                          "or fault address. " );
            }
        }

        if( remoteRecord == null ) {
            if( retryFault != null ) {
                throw retryFault;
            } 
            return null;
         }

        log.info( "valid record="+ remoteRecord);
        
        remoteRecord.resetExpireTime( wsContext.getTtl( provider ) );
          
        if( wsContext.isDbCacheOn( provider ) ) {

            DhtRouterMessage message =
                new DhtRouterMessage( DhtRouterMessage.UPDATE,
                                      remoteRecord, null );

            log.info( "DhtRouterMessage: " + message );
        
            this.notifyObservers( provider, message );
        }

        return remoteRecord;
    }

    private boolean isRecordValid( NativeRecord record ) {

        if( record.getNativeXml() == null 
            || record.getNativeXml().isEmpty() ) {

            return false;
        }
        return true;
    }
    
    public void addObserver( Object obj ){
        observerList.add( obj );
    }

    public void notifyObservers( String provider, Object arg){

        for( Iterator io = observerList.iterator(); io.hasNext(); ){
            
            WSContext ctx = (WSContext) io.next();
            
            log.info("updating context="+ ctx + " provider=" + provider );
            //r.update( this, arg );
            ctx.update( this, provider, arg );
        }
    }
}
