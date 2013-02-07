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
    protected NativeServer selectNextRemoteServer( String provider,
                                                   String service,
                                                   String namespace,
                                                   String accession ) {

        if ( rsc.isRemoteProxyOn() ) {

            log.info( " selecting next proxy..." );

            // register as interested
            // ----------------------

            log.info( " adding observer..." );
            this.addObserver( router );

            return router.getNextProxyServer( service, namespace, accession );
        }

        //return rsc.getNativeServerMap().get( service );
        return rsc.getNativeServer();

    }

    protected NativeRecord getNativeFromRemote ( String provider, 
                                                 String service, 
                                                 String ns, 
                                                 String ac 
                                                 ) throws ProxyFault {

        int retry = router.getMaxRetry();  
        NativeRecord remoteRecord = null;
          
        while ( retry > 0 && remoteRecord == null ) {    
            //RemoteServer rs = 
            NativeServer nativeServer = 
                selectNextRemoteServer( provider, service, ns, ac );

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
                throw fault;      
            } catch ( Exception ex ) {
                log.warn( "getNativeFromRemote: ex=" + ex.toString() );
                
            }
            
            log.info( "getNativeFromRemote: after got remoteRecord=" + 
                      remoteRecord );

            if( remoteRecord != null ) {      
                String natXml = remoteRecord.getNativeXml();

                log.info( "getNativeFromRemote: got natXml=" + natXml );

                if( natXml == null || natXml.length() == 0 ) {            
                    // remote site problem
                    // NOTE: should also drop on exception remote exception ???

                    log.info( "getNative: natXml is null/zero length. " );
                    this.setChanged(); // drop site from DHT

                    DhtRouterMessage message =
                        new DhtRouterMessage( DhtRouterMessage.DELETE,
                                              remoteRecord, nativeServer );

                    this.notifyObservers( message );
                    this.clearChanged();
                    remoteRecord = null;
                } else {
                    if( remoteRecord.getExpireTime() == null ) {

                        //*** remoteRecord is newly created from remote native 
                        remoteRecord.resetExpireTime(        
                            remoteRecord.getCreateTime(), rsc.getTtl() );

                        this.setChanged(); // update site from DHT

                        DhtRouterMessage message =
                            new DhtRouterMessage( DhtRouterMessage.UPDATE,
                                                  remoteRecord, nativeServer );

                        this.notifyObservers( message );
                        this.clearChanged();
                    } 
                }
            }

        }

        return remoteRecord;
    }

}
