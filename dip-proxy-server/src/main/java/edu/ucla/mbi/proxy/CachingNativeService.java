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

    private Log log = LogFactory.getLog( CachingService.class );

    protected String provider;
    private RemoteServerContext rsc;
    private Router router;

    private static McClient mcClient = WSContext.getMcClient();
    private static DxfRecordDAO dxfDAO = DipProxyDAO.getDxfRecordDAO();
    private static NativeRecordDAO nDAO = DipProxyDAO.getNativeRecordDAO();

    public CachingService( String provider, Router router, 
                           RemoteServerContext rsc ) 
    {
        this.provider = provider;
        this.router = router;
        this.rsc = rsc;
    }
    
    private CachingService() { }

    //--------------------------------------------------------------------------
    
    public NativeRecord getNative( String provider, String service, String ns,
                                   String ac ) throws ProxyFault {

        Date currentTime = Calendar.getInstance().getTime();
        String id = provider + "_" + service + "_" + ns + "_" + ac;

        log.info( "getNative(provider=" + provider + ")" );
        log.info( "         (router=" + router + ")" );
        log.info( "         (router.rsc="
                  + router.getRemoteServerContext().getProvider() + ")" );
        log.info( " ramCache on=" + rsc.isRamCacheOn() );
        log.info( " dbCache on=" + rsc.isDbCacheOn() );

        NativeRecord cacheRecord = null;
        boolean cacheExpired = false;
        
        NativeRecord remoteRecord = null;
        boolean remoteExpired = false;

        NativeRecord expiredRecord = null;
       
        String natXml = null;

        //*** fetch from memcached
        NativeRecord memcachedRec = null;
        String memcachedId = "NATIVE_" + provider + "_" + service + 
                             "_" + ns + "_" + ac;

        //*** retrieve from memcached
        if( rsc.isRamCacheOn() ) {
            try {
                memcachedRec = (NativeRecord)mcClient.fetch( memcachedId );
            } catch ( Exception ex ) {
                log.warn ( "FAULT " + Fault.CACHE_FAULT + ":" + 
                       Fault.getMessage( Fault.CACHE_FAULT ) + ":" + ex.toString() );
            }

            log.info( "getNative: memcachedRec=" + memcachedRec );
        
            if( memcachedRec != null ) {
                log.info( "getNative: memcachedRec != null. " );
                return memcachedRec;
            }	
        }

        //*** retrieve from local database
        if ( rsc.isDbCacheOn() ) { 

            // get cached copy of native record
            // ----------------------------------

            cacheRecord = DipProxyDAO.getNativeRecordDAO()
                                .find( provider, service, ns, ac );

            log.info( "Native rec: " + cacheRecord );

            if ( cacheRecord != null ) { // local record present

                natXml = cacheRecord.getNativeXml();

                Date expirationTime = cacheRecord.getExpireTime();
                boolean deleteRecord = false;

                log.info( "Native record: CT=" + cacheRecord.getCreateTime()
                          + " ET=" + expirationTime );

                if( natXml == null || natXml.length() == 0 ) {
                    DipProxyDAO.getNativeRecordDAO().delete( cacheRecord );
                    cacheRecord = null;
                } else {
                    if ( currentTime.after( expirationTime ) ) {
                        cacheExpired = true;
                        expiredRecord = cacheRecord;
                    } else {
                        //*** return valid record from local cache
                        log.info( "getNative: return from local cache." ); 

                        if( rsc.isRamCacheOn() ) {
                            log.info( "getNative: store cacheRecrod with " + 
                                      "memcachedId(" + memcachedId );
                            try {
                                mcClient.store( memcachedId, cacheRecord );
                            } catch ( Exception ex ) {
                                log.warn ( "FAULT " + Fault.CACHE_FAULT + ":" +
                                           Fault.getMessage( Fault.CACHE_FAULT ) + 
                                           ":" + ex.toString() );
                            }
                        }
                        return cacheRecord;
                    }
                }
            }
        }
       
        //*** retrieve from remote proxy server  
        if( rsc.isRemoteProxyOn() ) {                        
            int retry = router.getMaxRetry();            
            //NativeRecord expiredRemoteRec = null;

            while ( retry > 0 && remoteRecord == null ) {    
                // no valid local copy - try remote source(s)
                
                //RemoteServer rs = 
                //    selectNextRemoteServer( provider, service, ns, ac );

                log.info( " selecting next proxy..." );

                // register as interested
                // ----------------------

                log.info( " adding observer..." );
                this.addObserver( router );
                RemoteServer rs = router.getNextProxyServer( service, 
                //                              namespace, accession );
                                                ns, ac );
                                                    

                log.info( " selected rs=" + rs );
                log.info( " retries left=" + retry );
                retry--;
                
                try {
                    remoteRecord = rs.getNative( provider, service, ns, ac, 
                                                 rsc.getTimeout(), retry );
                    log.info( "getNative: remoteRecord=" + remoteRecord );
                } catch( ProxyFault fault ) {
                    log.warn("getNative: RemoteServer getNative() fault: " 
                             + fault.getFaultInfo().getMessage());                    

                    //throw fault;
                }

                if( remoteRecord != null ) {      
                
                    natXml = remoteRecord.getNativeXml();

                    Date queryTime = remoteRecord.getCreateTime();  // primary query
                
                    Calendar qCal = Calendar.getInstance();
                    qCal.setTime( queryTime );
                    qCal.add( Calendar.SECOND, rsc.getTtl() );
		
                    if( natXml == null && natXml.length() == 0 ) {            
                        // remote site problem
                        // NOTE: should also drop on exception remote exception ???

                        this.setChanged(); // drop site from DHT

                        DhtRouterMessage message =
                                new DhtRouterMessage( DhtRouterMessage.DELETE,
                                                      remoteRecord, rs );

                        this.notifyObservers( message );
                        this.clearChanged();

                    } else {
                    
                        if( currentTime.after( qCal.getTime() ) ) {
                            //*** remote record is expired
                            log.info( "getNative: remoteExpired=true. " );
                            if( expiredRecord == null ) {
                                expiredRecord = remoteRecord;
                                log.info( "getNative: got a remote expiredRec." );
                            } else {
                                if( expiredRecord.getExpireTime().after( 
                                    remoteRecord.getExpireTime() ) ) {

                                    //*** using more recentlly expired record
                                    expiredRecord = remoteRecord; 
                                }
                            }
                        } else {
                            //*** return remoteRecord  

                            //*** update to dbCache                            
                            if( rsc.isDbCacheOn() && 
                                ( cacheRecord == null || cacheExpired  ) ) {

                                log.info( "  CachingService: creating cache record" );
                                if( cacheRecord == null ) {
                                    cacheRecord = new NativeRecord( provider, service, ns, ac );
                                }
                                cacheRecord.setNativeXml( remoteRecord.getNativeXml() );

                                // NOTE: remoteRecord must specify time of the primary 
                                //       source query
                                Date remoteQueryTime = remoteRecord.getCreateTime();
                                cacheRecord.resetExpireTime( remoteQueryTime, rsc.getTtl() );

                                log.info( "  CachingService: rqt=" + remoteQueryTime );
                                                        
                                //*** store/update native record locall
                                DipProxyDAO.getNativeRecordDAO()
                                    .create( cacheRecord ); 

                            }

                            if( rsc.isRamCacheOn() ) {
                                //*** store to memcached               
                                log.info( "getNative: store remoteRecord with " +
                                          "memcachedId(" + memcachedId + ")." );
                                try {
                                    mcClient.store( memcachedId, remoteRecord );
                                } catch ( Exception ex ) {
                                    log.warn ( "FAULT " + Fault.CACHE_FAULT +
                                               ":" + Fault.getMessage( 
                                               Fault.CACHE_FAULT ) +
                                               ":" + ex.toString() );
                                }
                            }

                            return remoteRecord; 

                        }
                    }
                } 
            }
        }
        
        //*** retrieve from nativeServer


        //*** finally return a expiredRecord      
        if( expiredRecord != null ) {
            return expiredRecord;
        }
    
            /*
            if ( natXml == null || natXml.length() == 0 ) {

                // no remote site found - throw exception
                // NOTE: distinguish between no record vs error here ? 
                
                log.warn( "Get nativeXml null. " );

                if( rsc.isRemoteProxyOn() )
                if( cacheRecord ) {
                    if( expiredRemoteRec != null ) {
                        //*** Both cache and remote don't have a valid record \
                        //*** or expired record.
                        //log.info( "getNative: final natXml is null, throw UNKNOWN fault. ");
                        //throw FaultFactory.newInstance( Fault.UNKNOWN );
                        //throw FaultFactory.newInstance( Fault.NO_RECORD );
                    //} else {
                        //*** remoteRecord is a expired record
                        remoteRecord = expiredRemoteRec;
                        natXml = remoteRecord.getNativeXml();
                        remoteExpired = true;
                    }
                } else {
                    remoteRecord = null;
                } 
            }

            //*** update/store cacheRecord base on remoteRecord 
            if ( rsc.isDbCacheOn() && remoteRecord != null ) { 

                boolean updateCacheRecord = false;

                if ( cacheRecord == null ) {
                    //in this case, remoteRecord is not null    
                    
                    //*** create new local record based on remoteRecord
                
                    log.info( "  CachingService: creating cache record" );
		
                    cacheRecord = new NativeRecord( provider, service, ns, ac );
                    cacheRecord.setNativeXml( remoteRecord.getNativeXml() );
                
		            // NOTE: remoteRecord must specify time of the primary 
		            //       source query
		
                    Date remoteQueryTime;
                    if( remoteExpired ) {
		                remoteQueryTime = remoteRecord.getQueryTime();
                    } else {
                        remoteQueryTime = remoteRecord.getCreateTime();
                    }

		            cacheRecord.resetExpireTime( remoteQueryTime, rsc.getTtl() ); 
                    updateCacheRecord = true;

		            log.info( "  CachingService: rqt=" + remoteQueryTime );
		            log.info( "  CachingService: lqt=" + cacheRecord.getQueryTime() );
                }

                if ( cacheExpired ) {
                    //*** local cache is expired     
                    if ( !remoteExpired ) {  
                        //*** update cache record from valid remote record
                    
                        log.info( "  CachingService: updating cache record" );
                    
                        cacheRecord.setNativeXml( remoteRecord.getNativeXml() );
                    
                        // NOTE: remoteRecord must specify time of the primary
                        //       source query

                        Date remoteCreateTime = remoteRecord.getCreateTime();
                        cacheRecord.resetExpireTime( remoteCreateTime, rsc.getTtl() );
                        cacheExpired = false;
                        updateCacheRecord = true;        
                    } 

                    if( remoteExpired  
                        && remoteRecord.getExpireTime().after( 
                                    cacheRecord.getExpireTime() ) ) {

                        //*** update caceh record from expired remote record
                        cacheRecord.setNativeXml( remoteRecord.getNativeXml() );

                        // NOTE: remoteRecord must specify time of the primary
                        //       source query

                        Date remoteQueryTime = remoteRecord.getQueryTime();
                        cacheRecord.resetExpireTime( remoteQueryTime, rsc.getTtl() );   
                        updateCacheRecord = true;         
                    } 
                }
           
                if( updateCacheRecord ) { 
                    DipProxyDAO.getNativeRecordDAO()
                        .create( cacheRecord ); // store/update native record locally
                    log.info( "  notify observers: new record" );
                    this.setChanged();

                    DhtRouterMessage message =
                        new DhtRouterMessage( DhtRouterMessage.UPDATE, cacheRecord,
                                      null ); // self
                    this.notifyObservers( message );
                    this.clearChanged();
                }
                
                if ( rsc.getDebugLevel() == 1 ) {
                    log.info( " dropping native record = " + cacheRecord );
                    DipProxyDAO.getNativeRecordDAO().delete( cacheRecord );
                }

                /*
                //*** return expired or updated from remoteRecord
                if( cacheRecord != null && !cacheExpired ) {
                    log.info( "getNative: store cacheRecrod with memcachedId(" +
                              memcachedId );
                    try {
                        mcClient.store( memcachedId, cacheRecord );
                    } catch ( Exception ex ) {
                        log.warn ( "FAULT " + Fault.CACHE_FAULT +
                               ":" + Fault.getMessage( Fault.CACHE_FAULT ) +
                               ":" + ex.toString() );
                    }
                }
                return cacheRecord;
                
            }

            //*** return remoteRecord
            if( remoteRecord != null && !remoteExpired ) {
                 log.info( "getNative: store remoteRecord with memcachedId" +
                           "(" + memcachedId + ").");
                try {
                    mcClient.store( memcachedId, remoteRecord );
                } catch ( Exception ex ) {
                    log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                               ":" + Fault.getMessage( Fault.CACHE_FAULT ) + 
                               ":" + ex.toString() );
                }
            }

            return remoteRecord;   
            */      
        }

        /*
        //*** store to memcached 
        if( rsc.isCacheOn() ) {
            //*** return expired or updated from remoteRecord
            if( cacheRecord != null && !cacheExpired ) {
                log.info( "getNative: store cacheRecrod with memcachedId(" + 
                          memcachedId );
                try {
                    mcClient.store( memcachedId, cacheRecord );
                } catch ( Exception ex ) {
                    log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                               ":" + Fault.getMessage( Fault.CACHE_FAULT ) + 
                               ":" + ex.toString() );
                }
            }
            return cacheRecord;
        } else {	    
            // caching off - return remoteRecord
            if( remoteRecord != null && !remoteExpired ) {
                 log.info( "getNative: store remoteRecord with memcachedId" +
                           "(" + memcachedId + ").");
                try {
                    mcClient.store( memcachedId, remoteRecord );
                } catch ( Exception ex ) {
                    log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                               ":" + Fault.getMessage( Fault.CACHE_FAULT ) + 
                               ":" + ex.toString() );
                }
            }
            return remoteRecord;
        }
        */
    }
    
    //--------------------------------------------------------------------------
    
    private RemoteServer selectRemoteServer( RemoteServerContext rsc, 
                                             String service ) 
    {

        if ( rsc.isRemoteProxyOn() ) {
            return router.getLastProxyServer( service );
        }
        return router.getNativeServer( service );
    } 

}
