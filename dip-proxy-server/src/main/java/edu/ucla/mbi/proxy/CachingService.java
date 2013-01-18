package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * CachingServices:
 *  return a dxf DatasetType based on the query;
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

public class CachingService extends Observable {

    private Log log = LogFactory.getLog( CachingService.class );

    protected String provider;
    private RemoteServerContext rsc;
    private Router router;
    private McClient mcClient = WSContext.getMcClient();

    static DxfRecordDAO dxfDAO = DipProxyDAO.getDxfRecordDAO();
    static NativeRecordDAO nDAO = DipProxyDAO.getNativeRecordDAO();

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
        log.info( " cache on=" + rsc.isCacheOn() );

        NativeRecord cacheRecord = null;
        boolean cacheExpired = false;
        
        NativeRecord remoteRecord = null;
        boolean remoteExpired = false;
       
        String natXml = null;

        //*** fetch from memcached
        NativeRecord memcachedRec = null;
        String memcachedId = "NATIVE_" + provider + "_" + service + 
                             "_" + ns + "_" + ac;

        try {
            memcachedRec = (NativeRecord)mcClient.fetch( memcachedId );
        } catch ( Exception ex ) {
            log.warn ( "FAULT " + Fault.CACHE_FAULT + ":" + 
                       Fault.getMessage( 29 ) + ":" + ex.toString() );
        }

        log.info( "getNative: memcachedRec=" + memcachedRec );
        if( memcachedRec != null ) {
            log.info( "getNative: memcachedRec != null. " );
            return memcachedRec;
        }	

        //*** try to retrieve from local cache
        if ( rsc.isCacheOn() ) { 

            // get cached copy of native record
            // ----------------------------------

            cacheRecord = DipProxyDAO.getNativeRecordDAO()
                                .find( provider, service, ns, ac );

            log.info( "Native rec: " + cacheRecord );

            if ( cacheRecord != null ) { // local record present

                Date expirationTime = cacheRecord.getExpireTime();
		
                log.info( "Native record: CT=" + cacheRecord.getCreateTime()
                          + " ET=" + expirationTime );

                if ( currentTime.after( expirationTime ) ) {
                    cacheExpired = true;
                } else {
                    natXml = cacheRecord.getNativeXml();
                    if( natXml == null ) {
                        cacheExpired = true;
                    } else {
                        //*** return valid record from local cache
                        log.info( "getNative: return from local cache." ); 
                        log.info( "getNative: store cacheRecrod with memcachedId(" +
                                   memcachedId );
                        try {
                            mcClient.store( memcachedId, cacheRecord );
                        } catch ( Exception ex ) {
                            log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                                       ":" + Fault.getMessage( 29 ) + 
                                       ":" + ex.toString() );
                        }

                        return cacheRecord;
                    }
                }
            }
        }
        
        //*** try to retrieve from remote server
        if ( cacheRecord == null || cacheExpired ) {  
            // NOTE: includes empty cache and expired cache
            
            int retry = router.getMaxRetry();            
            NativeRecord expiredRemoteRec = null;

            while ( retry > 0 && remoteRecord == null ) {    
                // no valid local copy - try remote source(s)
                
                RemoteServer rs = 
                    selectNextRemoteServer( provider, service, ns, ac );
                
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

                    throw fault;
                }

                if( remoteRecord != null ) {      
                    Date queryTime = remoteRecord.getCreateTime();  // primary query
                
                    Calendar qCal = Calendar.getInstance();
                    qCal.setTime( queryTime );
                    qCal.add( Calendar.SECOND, rsc.getTtl() );
		
                    boolean messageDelete = false;

                    if( currentTime.after( qCal.getTime() ) ) {
                        log.info( "getNative: remoteExpired=true. " );
                        String expiredNatXml = remoteRecord.getNativeXml();
                        
                        if( expiredNatXml != null && expiredNatXml.length() > 0 ) {
                            if( expiredRemoteRec == null ) {
                                expiredRemoteRec = remoteRecord;
                                log.info( "getNative: got a remote expiredRec." );
                            } else {
                                if( expiredRemoteRec.getExpireTime().after( 
                                    remoteRecord.getExpireTime() ) ) {

                                    //*** using more recentlly expired record
                                    expiredRemoteRec = remoteRecord; 
                                }
                            }
                        } else {
                            messageDelete = true;
                        }
                    } else {
                        natXml = remoteRecord.getNativeXml();
    
                        if ( natXml == null || natXml.length() == 0 ) {
                            messageDelete = true;
                        }
                    }

                    if( messageDelete ) {
                        // remote site problem
                        // NOTE: should also drop on exception remote exception ???

                        this.setChanged(); // drop site from DHT
    
                        DhtRouterMessage message =
                                new DhtRouterMessage( DhtRouterMessage.DELETE,
                                                      remoteRecord, rs );

                        this.notifyObservers( message );
                        this.clearChanged();
                    }
                } 
            }
            
            if ( natXml == null || natXml.length() == 0 ) {

                // no remote site found - throw exception
                // NOTE: distinguish between no record vs error here ? 
                
                log.warn( "Get nativeXml null. " );

                if( cacheRecord == null ) {
                    if( expiredRemoteRec == null ) {
                        //*** Both cache and remote don't have a valid record \
                        //*** or expired record.
                        log.info( "getNative: final natXml is null, throw UNKNOWN fault. ");
                        throw FaultFactory.newInstance( Fault.UNKNOWN );
                    } else {
                        //*** remoteRecord is a expired record
                        remoteRecord = expiredRemoteRec;
                        natXml = remoteRecord.getNativeXml();
                        remoteExpired = true;
                    }
                } else {
                    remoteRecord = null;
                } 
            }
        }

        //*** update/store cacheRecord base on remoteRecord 
        if ( rsc.isCacheOn() && remoteRecord != null ) { 

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
        } 

        //*** store to memcached 
        if( rsc.isCacheOn() ) {
            //*** return expired or updated from remtoeRecord
            if( cacheRecord != null && !cacheExpired ) {
                log.info( "getNative: store cacheRecrod with memcachedId(" + 
                          memcachedId );
                try {
                    mcClient.store( memcachedId, cacheRecord );
                } catch ( Exception ex ) {
                    log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                               ":" + Fault.getMessage( 29 ) + 
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
                               ":" + Fault.getMessage( 29 ) + 
                               ":" + ex.toString() );
                }
            }
            return remoteRecord;
        }
    }
    
    //--------------------------------------------------------------------------

    public DatasetType getDxf( String provider, String service, String ns,
                               String ac, String detail 
                               ) throws ProxyFault {

        log.info( "getDxf(prv=" + provider + " srv=" + service + " det="
                  + detail + ")" );
        
        log.info( " cache on=" + rsc.isCacheOn() );

        DxfRecord dxfRecord = null;
        Date currentTime = Calendar.getInstance().getTime();
        boolean dxf_expired = false;
        DatasetType dxfRslt = null;

        //*** fetch from memcached
        DxfRecord memcachedRec = null;
        String memcachedId = "DXF_" + provider + "_" + service + "_" + ns + 
                             "_" + ac + "_" + detail;

        try {
            memcachedRec = (DxfRecord)mcClient.fetch( memcachedId );
        } catch ( Exception ex ) {
            log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                       ":" + Fault.getMessage( 29 ) + 
                       ":" + ex.toString() );
        }

        log.info( "getDxf: memcachedRec=" + memcachedRec );

        if( memcachedRec !=  null ) {
            dxfRslt = unmarshall( memcachedRec.getDxf() );
            return dxfRslt;
        }

        if ( rsc == null ) {
            log.warn("remote server is null.");
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        if ( rsc.isCacheOn() ) {
            
            try {
                
                // get cached copy of dxf record
                // ------------------------------
                
                dxfRecord = DipProxyDAO.getDxfRecordDAO()
                                .find( provider, service, ns, ac, detail );
                
                // check the expiration date
                // -------------------------
                
                if ( dxfRecord != null ) {
                    
                    Date expirationTime = dxfRecord.getExpireTime();
                    
                    log.info( "CachingService: dxf record CT="
                              + dxfRecord.getCreateTime() + " ET= "
                              + expirationTime );
                    
                    if ( currentTime.after( expirationTime ) ) {
                        dxf_expired = true;
                        dxfRecord.setCreateTime();
                    } 

                    dxfRslt = unmarshall( dxfRecord.getDxf() );
                        
                    if ( rsc.getDebugLevel() == 1 ) {
                        log.info( " dropping dxf record = " + dxfRecord );
                        DipProxyDAO.getDxfRecordDAO().delete( dxfRecord );
                    }

                    if( !dxf_expired ) {
                        if ( !dxfRslt.getNode().isEmpty() 
                            && !dxfRslt.getNode().get(0).getAc().equals("") ) {
                            
                            //*** store to memcached 
                            log.info( "getDxf: store cacheRecrod with memcachedId(" +
                                      memcachedId + ")" );
                            try {
                                mcClient.store( memcachedId, dxfRecord );
                            } catch ( Exception ex ) {
                                log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                                           ":" + Fault.getMessage( 29 ) + 
                                           ":" + ex.toString() );
                            }

                            return dxfRslt;
                        }else{
                            log.info( "CachingService: " +
                                      "dxf record is empty or there is no ac.");
                            DipProxyDAO.getDxfRecordDAO().delete( dxfRecord );                        
                        }
                    }
                } else {
                    log.info( "getDxf: dxfRecord==null. " );
                }
                
            } catch ( Exception e ) {
                log.warn( "getDxf(): for service " + service + 
                          " and ac " + ac + " exception: " + e.toString() );
                throw FaultFactory.newInstance( Fault.UNKNOWN );
            }
        }

        // valid dxf record not available when here ( null or expired )
        // -----------------------------------------
      
        NativeRecord nr = null;
        String nativeXml = null;
 
        try { 
            nr = getNative( provider, service, ns, ac );
            nativeXml = nr.getNativeXml();
        } catch ( ProxyFault fault ) {
            throw fault;
        } 

        
        //*** transform nativeXML to DXF
        // ---------------------------
        
        // NOTE: parametrize getTransformer() with detail here ?

        //**** nativeRecord is expired
        if ( currentTime.after( nr.getExpireTime() ) ) {
            if( dxf_expired ) {
                //*** nr is expired then return original expired dxf
                //DatasetType dxfRslt = unmarshall( dxfRecord.getDxf() );

                if ( !dxfRslt.getNode().isEmpty()
                     && !dxfRslt.getNode().get(0).getAc().equals("") ) 
                {
                    log.info( "getDxf: return expried dxf without memcached " + 
                              "storing. " );
                    return dxfRslt;
                } else {
                    //*** the expired dxf record is invalid, delete it
                    log.warn( "getDxf: expired dxf is invalid, discard it. ");
                    DipProxyDAO.getDxfRecordDAO().delete( dxfRecord ); 
                    throw FaultFactory.newInstance( Fault.UNKNOWN );
                }
            } else {
                //*** expired native cause expired dxf
                dxf_expired = true; //at this moment dxfRecord = null && nativeExpired
            } 
        } 
        
        //*** here nativeRecord is not expired
        //*** and dxfRecord==null || dxf_expired.

        //*** build dxf from nativeRecord
        RemoteServer rs = selectRemoteServer( rsc, service );

        DatasetType dxfResult = null;
        
        try {

            dxfResult = rs.buildDxf( nativeXml, ns, ac, 
                                     detail, provider, service );

            log.info( "getDxf: after buildDxf. dxfResult=" + dxfResult );
        } catch( ProxyFault se ) {
            log.warn( "getDxf(): transform error for service " + service +
                      " and ac " + ac + " exception: "+ se.toString());


            //*** return expired dxf
            if ( dxf_expired && !dxfRslt.getNode().isEmpty()
                 && !dxfRslt.getNode().get(0).getAc().equals("") ) {

                log.info( "getDxf: return expired dxf after transform error1" +
                          "and without memcached store. " );
                return dxfRslt;
            } else {
                throw FaultFactory.newInstance( Fault.TRANSFORM );
            }
        }

        // build and store dxf record
        // ---------------------------

        if ( dxfResult.getNode().isEmpty() 
             || dxfResult.getNode().get(0).getAc().equals("") ) {

            log.warn( "getDxf(): transformer error: dxf_record missing node " +
                      "or node ac is empty for service=" + service +
                      " and ac=" + ac + ".");

            //*** return expired dxf
            if ( dxf_expired && !dxfRslt.getNode().isEmpty()
                 && !dxfRslt.getNode().get(0).getAc().equals("") ) {
                
                log.info( "getDxf: return expired dxf after transform error2 " +
                          "and without memcached store. " );
                return dxfRslt;
            } else {
                throw FaultFactory.newInstance( Fault.TRANSFORM );
            }
        }
       
        //*** store newly built dxfRecord into local cache
        if ( rsc.isCacheOn() ) {
            try {
                
                // mashall DatasetType object into a string representation
                // --------------------------------------------------------
                String dxfString = marshall( dxfResult );

                if ( dxfRecord == null ) {
                    dxfRecord =
                        new DxfRecord( provider, service, ns, ac, detail );
                }

                dxfRecord.setDxf( dxfString );
                if( dxf_expired ) {
                    dxfRecord.resetExpireTime( nr.getQueryTime(), rsc.getTtl() );
                } else {
                    dxfRecord.resetExpireTime( rsc.getTtl() );
                }
                
                //log.info( " caching dxf: new expiration time=" + getExpireTime() );
                DipProxyDAO.getDxfRecordDAO().create( dxfRecord );

                if ( rsc.getDebugLevel() == 1 ) {
                    if ( rsc.getDebugLevel() == 1 ) {
                        log.info( " dropping dxf record = " + dxfRecord );
                        DipProxyDAO.getDxfRecordDAO().delete( dxfRecord );
                    }
                }
                
            } catch ( Exception e ) {
                log.warn( "getDxf(): for service " + service + 
                          " and ac " + ac + " exception: " + e.toString() );
                throw FaultFactory.newInstance( Fault.UNKNOWN );
            }
        }

        //*** store to memcached 
        if( dxfRecord !=  null ) {
            log.info( "getDxf: store cacheRecrod with memcachedId(" + 
                      memcachedId + ")" );
            try {
                mcClient.store( memcachedId, dxfRecord );
            } catch ( Exception ex ) {
                log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                           ":" + Fault.getMessage( 29 ) + 
                           ":" + ex.toString() );
            }
        }

        return dxfResult;
    }
    
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
            return router.getNextProxyServer( service, 
                                              namespace, accession );
        }
        return router.getNativeServer(service);
    }
    
    private RemoteServer selectRemoteServer( RemoteServerContext rsc, 
                                             String service ) 
    {

        if ( rsc.isRemoteProxyOn() ) {
            return router.getLastProxyServer( service );
        }
        return router.getNativeServer( service );
    } 

    private String marshall( DatasetType record ) throws ProxyFault {

        try {

            edu.ucla.mbi.dxf14.ObjectFactory dofDxf =
                    new edu.ucla.mbi.dxf14.ObjectFactory();

            JAXBContext dxfJc = DxfJAXBContext.getDxfContext();

            Marshaller dxfMarshaller = dxfJc.createMarshaller();
            
            java.io.StringWriter swResult = new StringWriter();
            
            dxfMarshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );
            dxfMarshaller.marshal( dofDxf.createDataset( record ), swResult );

            String result = swResult.toString();

            //log.info( "marshall: result=" + result );
            return result;

        } catch ( Exception e ) {
            log.warn( "marshall(): exception: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
    }

    private DatasetType unmarshall( String dxfString ) 
        throws ProxyFault {
        
        try {
            // unmarshall into DatasetType
            // ----------------------------
            
            JAXBContext dxfJc = DxfJAXBContext.getDxfContext();

            Unmarshaller dxfUnmarshaller = dxfJc.createUnmarshaller();

            StringReader sr = new StringReader( dxfString );

            JAXBElement<DatasetType> datasetElement =
                dxfUnmarshaller.unmarshal( new StreamSource( sr ),
                                           DatasetType.class );
            
            DatasetType dxfResult = datasetElement.getValue();
            
            if ( dxfResult.getNode().isEmpty() ) {
                throw FaultFactory.newInstance( Fault.NO_RECORD );
            } else {
                return dxfResult;
            }
            
        } catch ( Exception e ) {
            log.warn( "unmarshall(): exception: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
    }
}
