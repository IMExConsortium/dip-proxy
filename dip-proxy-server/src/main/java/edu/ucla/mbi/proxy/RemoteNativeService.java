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
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.fault.*;

class RemoteNativeService { 

    private Log log = LogFactory.getLog( RemoteNativeService.class );

    private WSContext wsContext;

    private List<Router> observerList = new ArrayList<Router>();

    public RemoteNativeService() { }
    
    public void setWsContext( WSContext context ) {
        this.wsContext = context;
    }

    private RemoteServerContext getRsc( String provider ){
        return wsContext.getServerContext(  provider );
    }
    
    //--------------------------------------------------------------------------
        
    private  NativeServer selectNextRemoteServer( String provider,
                                                  String service,
                                                  String namespace,
                                                  String accession ) {

        if ( getRsc(provider).isRemoteProxyOn() ) {
            
            log.info( " selecting next proxy..." );

            // register as interested
            // ----------------------

            log.info( " adding observer..." );
            this.addObserver( getRsc(provider).getRouter() );

            log.info( "before router getNextProxyServer. " );
            return getRsc(provider).getRouter().getNextProxyServer( provider, service, 
                                              namespace, accession );
        }

        return getRsc(provider).getNativeServer();

    }

    public NativeRecord getNativeFromRemote ( String provider, 
                                              String service, 
                                              String ns, 
                                              String ac 
                                              ) throws ServerFault {

        int retry = getRsc(provider).getMaxRetry();
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

            
            if( getRsc(provider).isRemoteProxyOn() && retry > 0 ) {
               
                if( !observerList.contains( getRsc(provider).getRouter() ) ) {
                    this.addObserver( getRsc(provider).getRouter() );
                } 
                nativeServer = getRsc(provider).getRouter().getNextProxyServer( provider, service,
                                                          ns, ac );
                log.info( "getNativeFromRemote: nativeServer came from proxy. " );
            } else {
                    
                //*** last retry or no proxy 
                nativeServer = getRsc(provider).getNativeServer();
                log.info( "getNativeFromRemote: nativeServer came from native. " );
                
            }

            try {                
                remoteRecord = nativeServer.getNative( 
                     provider, service, ns, ac, getRsc(provider).getTimeout() );

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
                faultyRecord.resetExpireTime( getRsc(provider).getTtl() );  
     
                DhtRouterMessage message =
                    new DhtRouterMessage( DhtRouterMessage.DELETE,
                                          faultyRecord, nativeServer );
                
                this.notifyObservers( message );
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
        
        remoteRecord.resetExpireTime( getRsc(provider).getTtl() );
          
        if( getRsc(provider).isDbCacheOn() ) {

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
            || record.getNativeXml().isEmpty() ) {

            return false;
        }
        return true;
    }
    
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
