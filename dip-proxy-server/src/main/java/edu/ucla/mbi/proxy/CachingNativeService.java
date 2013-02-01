package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * CachingNativeServices:
 *  returns a native record;
 *
 *======================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.transform.stream.StreamSource;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.orm.*;
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.cache.orm.*;
import edu.ucla.mbi.proxy.router.*;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.server.*;
import edu.ucla.mbi.util.cache.*;

class CachingNativeService extends Observable {

    private Log log = LogFactory.getLog( CachingNativeService.class );

    protected String provider;
    protected RemoteServerContext rsc;
    protected Router router;

    protected static McClient mcClient = WSContext.getMcClient();
    private static NativeRecordDAO nDAO = DipProxyDAO.getNativeRecordDAO();

    
    protected CachingNativeService( String provider, Router router, 
                                 RemoteServerContext rsc ) 
    {
        this.provider = provider;
        this.router = router;
        this.rsc = rsc;
    }
    
    protected CachingNativeService() { }
    
    //--------------------------------------------------------------------------
    
    public NativeRecord getNative( String provider, String service, String ns,
                                   String ac ) throws ProxyFault {

        String id = provider + "_" + service + "_" + ns + "_" + ac;

        log.info( "getNative(provider=" + provider + ")" );
        log.info( "         (router=" + router + ")" );
        log.info( "         (router.rsc="
                  + router.getRemoteServerContext().getProvider() + ")" );
        log.info( " ramCacheOn=" + rsc.isRamCacheOn() );
        log.info( " dbCacheon=" + rsc.isDbCacheOn() );

        NativeRecord cacheRecord = null;
        boolean cacheExpired = false;
        
        NativeRecord remoteRecord = null;
        boolean remoteExpired = false;
        ProxyFault proxyFault = null;

        String natXml = null;
        NativeRecord expiredRecord = null;

        String memcachedId = "NATIVE_" + provider + "_" + service + 
                             "_" + ns + "_" + ac;

        //*** retrieve from memcached
        if( rsc.isRamCacheOn() ) {
            NativeRecord memcachedRec = null;
            try {
                memcachedRec = (NativeRecord)mcClient.fetch( memcachedId );
            } catch ( Exception ex ) {
                log.warn ( "FAULT " + Fault.CACHE_FAULT + ":" + 
                           Fault.getMessage( Fault.CACHE_FAULT ) + 
                           ":" + ex.toString() );
            }

            log.info( "getNative: memcachedRec=" + memcachedRec );
        
            if( memcachedRec != null ) {
                log.info( "getNative: memcachedRec != null. " );
                return memcachedRec;
            }	
        }

        //*** retrieve from local database
        if ( rsc.isDbCacheOn() ) { 

            try {
                cacheRecord = DipProxyDAO.getNativeRecordDAO()
                                        .find( provider, service, ns, ac );
            } catch ( DAOException ex ) {
                proxyFault = FaultFactory.newInstance( Fault.TRANSACTION );
            } 

            if ( cacheRecord != null ) { // local record present

                natXml = cacheRecord.getNativeXml();

                if( natXml == null || natXml.length() == 0 ) {
                    DipProxyDAO.getNativeRecordDAO().delete( cacheRecord );
                    cacheRecord = null;
                } else {
                    Date expirationTime = cacheRecord.getExpireTime();
                    Date currentTime = Calendar.getInstance().getTime();

                    log.info( "Native record: CT=" + 
                              cacheRecord.getCreateTime() +
                              " ET=" + expirationTime );

                    if ( currentTime.after( expirationTime ) ) {
                        cacheExpired = true;
                        expiredRecord = cacheRecord;
                    } else {
                        //*** return valid record from dbCache
                        log.info( "getNative: return from dbCache." ); 

                        if( rsc.isRamCacheOn() ) {
                            memcachedStore ( memcachedId, cacheRecord );
                        }

                        return cacheRecord;
                    }
                }
            }
        }
      
        //*** valid native record not available here ( null or expired ) 

        //*** retrieve from remote proxy server or native server  
        int retry = router.getMaxRetry();            
        while ( retry > 0 && remoteRecord == null ) {    
                
            RemoteServer rs = 
                selectNextRemoteServer( provider, service, ns, ac );

            log.info( " selected rs=" + rs );
            log.info( " retries left=" + retry );
            retry--;
                
            try {
                remoteRecord = rs.getNative( provider, service, ns, ac, 
                                             rsc.getTimeout(), retry );
            } catch( ProxyFault fault ) {
                log.warn( "getNative: RemoteServer getNative() fault: " + 
                          fault.getFaultInfo().getMessage()); 
                proxyFault = fault;        
            }

            if( remoteRecord != null ) {      
                natXml = remoteRecord.getNativeXml();

                if( natXml == null || natXml.length() == 0 ) {            
                    // remote site problem
                    // NOTE: should also drop on exception remote exception ???

                    log.info( "getNative: natXml is null/zero length. " );
                    this.setChanged(); // drop site from DHT

                    DhtRouterMessage message =
                        new DhtRouterMessage( DhtRouterMessage.DELETE,
                                              remoteRecord, rs );

                    this.notifyObservers( message );
                    this.clearChanged();
                    remoteRecord = null;

                } else {
                    Date currentTime = Calendar.getInstance().getTime();

                    if( remoteRecord.getExpireTime() == null ) {
                        //*** remoteRecord is newly created 
                        remoteRecord.resetExpireTime( 
                            remoteRecord.getCreateTime(), rsc.getTtl() );
                    }

                    
                    if( currentTime.after( remoteRecord.getExpireTime() ) ) {
                        //*** remote record is expired
                        log.info( "getNative: remoteExpired=true. " );
                        if( expiredRecord == null ) {
                            expiredRecord = remoteRecord;
                            log.info( "getNative: got a remote expiredRec." );
                            
                        } else {
                            //*** select more recentlly expired record
                            if( expiredRecord.getExpireTime()
                                    .after( remoteRecord.getExpireTime() ) ) {

                                //*** update expired from dbCache
                                if( rsc.isDbCacheOn() ) {
                                    expiredRecord.setNativeXml( 
                                        remoteRecord.getNativeXml() );

                                    expiredRecord.resetExpireTime ( 
                                        remoteRecord.getQueryTime(), 
                                        rsc.getTtl() );

                                    DipProxyDAO.getNativeRecordDAO()
                                                .create( expiredRecord );

                                }
                                expiredRecord = remoteRecord;
                            } 
                        }
                        
                        return expiredRecord;
                    } else {
                        //*** return remoteRecord  
                        
                        //*** dbCache update                           
                        if( rsc.isDbCacheOn() ) {

                            if( cacheRecord == null ) {
                                cacheRecord = new NativeRecord( provider, 
                                                                service, ns, ac );
                            }

                            dbCacheUpdate ( cacheRecord, remoteRecord );
                            // need update here ?????????????
                        }
                        
                        //*** memcached store
                        if( rsc.isRamCacheOn() ) {
                            memcachedStore ( memcachedId, remoteRecord );
                        }

                        return remoteRecord; 

                    }
                } 
            } else {
                log.info( "getNative: remoteRecord is null. " );
            } 
        }
        
        //*** finally return a expiredRecord      
        if( expiredRecord != null ) {
            log.info( "getNative: return expiredRecord=" + expiredRecord );
            return expiredRecord;
        } else if ( proxyFault != null ) {
            log.info( "getNative: throw a proxyFault. " );
            throw proxyFault;
        } else { 
            log.info( "getNative: return a null. " );
            return null;
        }
    }
    
    //--------------------------------------------------------------------------
        
    private RemoteServer selectNextRemoteServer( String provider,
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

        return router.getNativeServer(service);

    }

    protected void memcachedStore ( String memcachedId, Object record ) {

        log.info( "getNative: store cacheRecrod with " + 
                  "memcachedId(" + memcachedId );
                            
        try {
            mcClient.store( memcachedId, record );
        } catch ( Exception ex ) {
            log.warn ( "FAULT " + Fault.CACHE_FAULT + ":" +
                       Fault.getMessage( Fault.CACHE_FAULT ) + 
                       ":" + ex.toString() );
        }
    }

    private void dbCacheUpdate ( NativeRecord oldRec, NativeRecord newRec ) {
        log.info( "  CachingService: create/update cache record" );
                            
        oldRec.setNativeXml( newRec.getNativeXml() );

        // NOTE: remoteRecord must specify time of the primary 
        //       source query

        Date queryTime = newRec.getCreateTime();
        oldRec.resetExpireTime( queryTime, rsc.getTtl() );

        log.info( "CachingService: rqt=" + queryTime );

        //*** store/update native record locall
        DipProxyDAO.getNativeRecordDAO().create( oldRec );

    }
    
}
