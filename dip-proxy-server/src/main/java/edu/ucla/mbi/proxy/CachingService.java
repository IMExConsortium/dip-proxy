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

public class CachingService extends RemoteNativeService {

    private Log log = LogFactory.getLog( CachingService.class );

    private static NativeRecordDAO nDAO = DipProxyDAO.getNativeRecordDAO();    
    private static DxfRecordDAO dxfDAO = DipProxyDAO.getDxfRecordDAO();
    private static McClient mcClient = WSContext.getMcClient();

    public CachingService( String provider, 
                           Router router, 
                           RemoteServerContext rsc ) {

        super( provider, router, rsc );
    }
    
    private CachingService() { }

    //--------------------------------------------------------------------------

    public NativeRecord getNative( String provider, 
                                   String service, 
                                   String ns,
                                   String ac 
                                   ) throws ProxyFault {

        String id = provider + "_" + service + "_" + ns + "_" + ac;

        log.info( "getNative(provider=" + provider + ")" );
        log.info( "         (router=" + router + ")" );
        log.info( "         (router.rsc="
                  + router.getRemoteServerContext().getProvider() + ")" );
        log.info( " ramCacheOn=" + rsc.isRamCacheOn() );
        log.info( " dbCacheon=" + rsc.isDbCacheOn() );


        NativeRecord nativeRecord = null;

        NativeRecord remoteRecord = null;
        NativeRecord expiredRecord = null;
        
        //boolean cacheExpired = false;
        //boolean remoteExpired = false;
 
        ProxyFault proxyFault = null;

        Date currentTime = Calendar.getInstance().getTime();
        
        String memcachedId = "NATIVE_" + provider + "_" + service +
                             "_" + ns + "_" + ac;

        //*** retrieve from memcached
        
        if( rsc.isRamCacheOn() ) {
            NativeRecord memcachedRec = null;
            try {
                memcachedRec = (NativeRecord)mcClient.fetch( memcachedId );
            } catch ( Exception ex ) {
                //log.warn ( "FAULT " + Fault.CACHE_FAULT + ":" +
                //           Fault.getMessage( Fault.CACHE_FAULT ) +
                //           ":" + ex.toString() );
            }

            log.info( "getNative: memcachedRec=" + memcachedRec );

            /*
            if( memcachedRec != null ) {
                log.info( "getNative: memcachedRec != null. " );
                return memcachedRec;
            }
            */
            
            if( memcachedRec != null ) {
                nativeRecord = memcachedRec;
            }
            
        }

        //*** retrieve from local database
        if ( nativeRecord == null && rsc.isDbCacheOn() ) {

            NativeRecord cacheRecord = null;

            try {
                cacheRecord = DipProxyDAO.getNativeRecordDAO()
                    .find( provider, service, ns, ac );
            } catch ( DAOException ex ) {
                proxyFault = FaultFactory.newInstance( Fault.TRANSACTION );
            }

            if ( cacheRecord != null ) { // local record present

                String natXml = cacheRecord.getNativeXml();
                
                if( natXml == null || natXml.isEmpty() ) {
                    DipProxyDAO.getNativeRecordDAO().delete( cacheRecord );
                    
                    // remove dht record here ?
                    
                    cacheRecord = null;
                } else {
                    Date expirationTime = cacheRecord.getExpireTime();
            
                    log.info( "Native record: CT=" +
                              cacheRecord.getCreateTime() +
                              " ET=" + expirationTime );

                    if ( currentTime.after( expirationTime ) ) {
                        //cacheExpired = true;
                        expiredRecord = cacheRecord;
                    } else {
                        //*** return valid record from dbCache
                        log.info( "getNative: return from dbCache." );
                        /*
                        if( rsc.isRamCacheOn() ) {
                            memcachedStore ( memcachedId, cacheRecord );
                        }
                        */
                        nativeRecord = cacheRecord;
                    }
                }
            }
        }

        //*** valid native record not available here ( null or expired ) 
        //*** retrieve from remote proxy server or native server  

        log.info( "getNative: before getNativeFromRemote. " );
        
        if( nativeRecord == null ){
            remoteRecord = getNativeFromRemote ( provider, service, ns, ac );
        }
        
        if( remoteRecord != null ) {
            if( currentTime.after( remoteRecord.getExpireTime() ) ) {
                //*** remote record is expired
                log.info( "getNative: remoteExpired=true. " );

                //*** select more recently expired record
                if(  expiredRecord == null ||
                     remoteRecord.getQueryTime()
                     .after( expiredRecord.getQueryTime() ) ){
                    
                    //*** update expiredRecord from remote
                 
                    if( expiredRecord != null ){
                        remoteRecord.setId( expiredRecord.getId() );
                    }
                    
                    expiredRecord = remoteRecord;
                }
            } else {
                nativeRecord = remoteRecord;
            }
        }

        if( nativeRecord != null ){
            //*** dbCache update                           
            if( rsc.isDbCacheOn() ) {               
                DipProxyDAO.getNativeRecordDAO().create( nativeRecord );
            }

            //*** memcached store
            if( rsc.isRamCacheOn() ) {
                memcachedStore ( memcachedId, nativeRecord );
            }
            return remoteRecord;
        }

        //*** finally return a expiredRecord      
        if( expiredRecord != null ){

            if( rsc.isDbCacheOn() ){
                DipProxyDAO.getNativeRecordDAO().create( expiredRecord );
            }
            
            //*** remote is null return expired record from dbCache 
            return expiredRecord;
        } else if ( proxyFault != null ) {
            log.warn( "getNative: throw a proxyFault. " );
            throw proxyFault;
        } else {
            log.info( "getNative: return a null. " );
            return null;
        }
    }

    //--------------------------------------------------------------------------

    public DatasetType getDatasetType( DxfRecord dxfRecord )
        throws ProxyFault{

        if( dxfRecord != null && dxfRecord.getDxf() != null 
            && !dxfRecord.getDxf().isEmpty() ) {

            return unmarshall( dxfRecord.getDxf() );

        } 
        return null;
    }
    
    //--------------------------------------------------------------------------

    public DxfRecord getDxfRecord( String provider,
                                   String service,
                                   String ns,
                                   String ac,
                                   String detail
                                   ) throws ProxyFault {
        
        DxfRecord dxfRecord = null;
        DxfRecord expiredDxf = null;


        
        
        boolean dxfExpired = false;

        DatasetType dxfResult = null;

        String dxfString = null;

        ProxyFault proxyFault = null;
        String memcachedId = "DXF_" + provider + "_" + service + "_" + ns +
                             "_" + ac + "_" + detail;

        Date currentTime = Calendar.getInstance().getTime();
        
        // test memcached; return record if present

        //*** retrieve from memcached

        if( rsc.isRamCacheOn() ){
            DxfRecord memcachedRec = null;
            try {
                memcachedRec = (DxfRecord)mcClient.fetch( memcachedId );
            } catch ( Exception ex ) {
                proxyFault = FaultFactory.newInstance( Fault.CACHE_FAULT );
            }

            log.info( "getDxf: memcachedRec=" + memcachedRec );

            if( memcachedRec !=  null ) {
                //*** return a valid result from memcached
                dxfRecord = memcachedRec;
            }
        }


        // test dbcachce; return record if not expired;
        // if expired store the record for later;
        
        //*** retrieve from local database 
        if( dxfRecord == null && rsc.isDbCacheOn() ){
            
            DxfRecord cacheDxfRecord = null;
            try {
                cacheDxfRecord = DipProxyDAO.getDxfRecordDAO()
                    .find( provider, service, ns, ac, detail );
            } catch ( DAOException ex ) {
                proxyFault = FaultFactory.newInstance( Fault.TRANSACTION );
            }     

            if( cacheDxfRecord != null ) {
                
                if( isDxfValid ( cacheDxfRecord.getDxf() ) ) {
                    Date expirationTime = cacheDxfRecord.getExpireTime();
                    
                    log.info( "CachingService: dxf record CT=" +
                              cacheDxfRecord.getCreateTime() + " ET= " +
                              expirationTime );

                    if( currentTime.after( expirationTime ) ) {
                        dxfExpired = true;
                        expiredDxf = cacheDxfRecord;
                    } else {
                        dxfRecord = cacheDxfRecord;
                    }
                } else {
                    try {
                        DipProxyDAO.getDxfRecordDAO().delete( dxfRecord );
                    } catch ( DAOException ex ) {
                        proxyFault = FaultFactory
                            .newInstance( Fault.TRANSACTION );
                    }
                }
            }
        }

        // get native record; if not expired make dxf;
        // if dxf valid store in dbcache & memchached then return 

        //*** retrieve from native, here dxfRecord==null|expired              

        if( dxfRecord == null ) { 
            
            NativeRecord nativeRecord = null;
            String nativeXml = null;
            
            try { 
                nativeRecord  = getNative( provider, service, ns, ac );
            } catch ( ProxyFault fault ) {
                log.warn( "getDxf: getNative fault. " ); 
                proxyFault = fault;
            } 

            if( nativeRecord != null ){
                try{ 
                    DxfRecord dxfr =  buildDxfRecord( nativeRecord, detail );
                    
                    if( currentTime.after( nativeRecord.getExpireTime() ) ) {
                        
                        //*** nativeRecord is expired
                        if(  expiredDxf == null ||
                             nativeRecord.getQueryTime()
                             .after ( expiredDxf.getQueryTime() ) ) {
                
                            if( expiredDxf!= null) {
                                dxfr.setId( expiredDxf.getId() );
                            }
                            expiredDxf = dxfr;
                        }
                    } else {
                        dxfRecord = dxfr;
                    }
                } catch ( ProxyFault fault ) {
                    log.warn( "getDxf: buildDxfRecord. " );
                    proxyFault = fault;
                }
            }    
        }
        
        if( dxfRecord != null ) {

            if( rsc.isDbCacheOn() ) {
                DipProxyDAO.getDxfRecordDAO().create( dxfRecord );
            }
            
            if( rsc.isRamCacheOn() ) {
                memcachedStore ( memcachedId, dxfRecord );
            }

            return dxfRecord;
        }
        
        if( expiredDxf != null ){
            
            if( rsc.isDbCacheOn() ){
                DipProxyDAO.getDxfRecordDAO().create( expiredDxf ); 
            }
            
            return expiredDxf;

        } else if ( proxyFault != null ) {
            log.warn( "getDxf: throw a proxyFault. " );
            throw proxyFault;
        } else {
            log.info( "getDxf: return a null. " );
            return null;
        }
    }

    
    //--------------------------------------------------------------------------

    private DxfRecord buildDxfRecord( NativeRecord nativeRecord,
                                      String detail ) 
        throws ProxyFault{
        
        DxfRecord dxfRecord = null;
        
        DatasetType dxfResult = null;
        ProxyFault proxyFault = null;

        String provider = nativeRecord.getProvider();
        String service = nativeRecord.getService();
        String ns = nativeRecord.getNs();
        String ac = nativeRecord.getAc();
        String nativeXml = nativeRecord.getNativeXml();
        
        
        if( !dxfExpired || nativeIsMostRecentExpired ) {
            try {
                ProxyDxfTransformer pdt = new ProxyDxfTransformer();
                dxfResult = pdt.buildDxf( nativeXml, ns,ac, detail,
                                          provider, service );
                
            } catch ( ProxyFault fault ) {
                log.warn( "getDxf: transform and build dxf fault. " );
                proxyFault = fault;
            }
        }
        
        if ( isDxfDatasetValid( dxfResult )){
            
            String dxfString = null;
            
            //*** mashall DatasetType object into a string representation
            try {
                dxfString = marshall( dxfResult );
            } catch ( ProxyFault fault ) {
                log.warn( "getDxf: marshall fault. " );
                proxyFault = fault;
            }
               
            dxfRecord = new DxfRecord( provider, service, ns,
                                       ac, detail );                
            
            dxfRecord.setDxf( dxfString );
            dxfRecord.resetExpireTime ( nativeRecord.getQueryTime(),
                                        rsc.getTtl() );                
        }
 
        return dxfRecord;
    }


    //-------------------------------------------------------------------------- 

    boolean isDxfValid( String dxfString ) throws ProxyFault { 

        DatasetType dxfResult = unmarshall( dxfString );
                
        if( dxfResult != null ) {
            if ( !dxfResult.getNode().isEmpty()
                 && !dxfResult.getNode().get(0).getAc().equals("") ) {

               return true; 
            } 
        }
        
        return false;
    }

    //--------------------------------------------------------------------------
    
    boolean isDxfDatasetValid( DatasetType  dxfDataset ) throws ProxyFault { ;
                
        if( dxfDataset != null 
            && !dxfDataset.getNode().isEmpty()
            && !dxfDataset.getNode().get(0).getAc().equals("") ) {
            return true; 
        } 
        
        return false;
    }
    
    //--------------------------------------------------------------------------

    public DatasetType getDxf( String provider, 
                               String service, 
                               String ns,
                               String ac, 
                               String detail 
                               ) throws ProxyFault {

        log.info( "getDxf(prv=" + provider + " srv=" + service + " det="
                  + detail + ")" );
        

        boolean dxfExpired = false;
        String dxfString = null;
        DxfRecord dxfRecord = null;
        DxfRecord expiredDxf = null;
        DatasetType dxfResult = null;
        DatasetType expiredResult = null;

        ProxyFault proxyFault = null;
        String memcachedId = "DXF_" + provider + "_" + service + "_" + ns + 
                             "_" + ac + "_" + detail;

        if ( rsc == null ) {
            log.warn("remote server is null.");
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        //*** retrieve from memcached
        if( rsc.isRamCacheOn() ) {
            DxfRecord memcachedRec = null;
            try {
                memcachedRec = (DxfRecord)mcClient.fetch( memcachedId );
            } catch ( Exception ex ) {
                proxyFault = FaultFactory.newInstance( Fault.CACHE_FAULT );
            }
        
            log.info( "getDxf: memcachedRec=" + memcachedRec );

            if( memcachedRec !=  null ) {
                //*** return a valid result from memcached
                dxfRecord = memcachedRec;
                try {
                    dxfResult = unmarshall( dxfRecord.getDxf() );
                    return dxfResult;
                } catch ( ProxyFault fault ) {
                    proxyFault = fault;
                }
            }
        }
   
        //*** retrieve from local database 
        if ( rsc.isDbCacheOn() ) {
            
            try {
                dxfRecord = DipProxyDAO.getDxfRecordDAO()
                                .find( provider, service, ns, ac, detail );
            } catch ( DAOException ex ) {
                proxyFault = FaultFactory.newInstance( Fault.TRANSACTION );
            }     
                
            if( dxfRecord != null ) {
                try {    
                    dxfResult = unmarshall( dxfRecord.getDxf() );
                } catch ( ProxyFault fault ) {
                    proxyFault = fault;
                }

                if( dxfResult != null ) {
                    if ( dxfResult.getNode().isEmpty()
                         || dxfResult.getNode().get(0).getAc().equals("") ) {

                        log.info( "CachingService: dxf record is invalid.");

                        try {
                            DipProxyDAO.getDxfRecordDAO().delete( dxfRecord );
                        } catch ( DAOException ex ) {
                            proxyFault = FaultFactory.newInstance( 
                                                        Fault.TRANSACTION );
                        }        

                        dxfRecord = null;
                        dxfResult = null;

                    } else {
                        Date expirationTime = dxfRecord.getExpireTime();
                        Date currentTime = Calendar.getInstance().getTime();

                        log.info( "CachingService: dxf record CT=" +
                                  dxfRecord.getCreateTime() + " ET= " +
                                  expirationTime );

                        if ( currentTime.after( expirationTime ) ) {
                            dxfExpired = true;
                            expiredDxf = dxfRecord;
                            dxfRecord = null;
                            expiredResult = dxfResult;
                            dxfResult = null;
                        } else {
                            //*** return valid record from dbCache
                            if( rsc.isRamCacheOn() ) {
                                memcachedStore ( memcachedId, dxfRecord );
                            }
                            //*** return a valid result from dbCache
                            return dxfResult;
                        }
                    }
                }
            }
        }

        //*** retrieve from native, here dxfRecord==null|expired      
        
        NativeRecord nr = null;
        String nativeXml = null;
        boolean nativeExpired = false;
 
        try { 
            nr = getNative( provider, service, ns, ac );
        } catch ( ProxyFault fault ) {
            log.warn( "getDxf: getNative fault. " ); 
            proxyFault = fault;
        } 

        if( nr != null ) {
            nativeXml = nr.getNativeXml();
            Date currentTime = Calendar.getInstance().getTime();

            if ( currentTime.after( nr.getExpireTime() ) ) {
                //*** nr is expired
                nativeExpired = true;

                if( dxfExpired 
                    && expiredDxf.getQueryTime().after( 
                        nr.getQueryTime() ) ) {
                        
                    //*** return expired coming from dbCache
                    return expiredResult;
                } 
            } 
        
            //*** get dxf from remote proxy or remote native using valid/expired nr
            try {

                ProxyDxfTransformer pdt = new ProxyDxfTransformer();
                dxfResult = pdt.buildDxf( nativeXml, ns,ac, detail, 
                                          provider, service );
                
            } catch ( ProxyFault fault ) {
                log.warn( "getDxf: transform and build dxf fault. " );
                proxyFault = fault;
            }
        }
            
        if ( dxfResult != null ) {

            //*** mashall DatasetType object into a string representation
            try {
                dxfString = marshall( dxfResult );
            } catch ( ProxyFault fault ) {
                log.warn( "getDxf: marshall fault. " );
                proxyFault = fault;
            }
   
            if ( dxfRecord == null ) {
                dxfRecord = new DxfRecord( provider, service, ns, ac, detail );
            }

            dxfRecord.setQueryTime( nr.getQueryTime() ); 
            
            dxfRecord.setDxf( dxfString );
            dxfRecord.resetExpireTime ( dxfRecord.getQueryTime(),
                                        rsc.getTtl() );
            
            if( !nativeExpired && rsc.isRamCacheOn() ) {
                memcachedStore ( memcachedId, dxfRecord );
            }

            if( rsc.isDbCacheOn() ) {
                DipProxyDAO.getDxfRecordDAO().create( dxfRecord );
            }
            
            //*** return valid/expired dxf coming from remote       
            return dxfResult;
        }
        
        if( expiredResult != null ) {
            //*** return expired dxf coming from dbCache, here remote is null
            return expiredResult;
        } else if ( proxyFault != null ) {
            log.warn( "getDxf: throw proxyFault. " );
            throw proxyFault;
        } else {
            return null;
        }
    }

    private void memcachedStore ( String memcachedId, Object record ) {

        log.info( "getNative: store cacheRecrod with " +
                  "memcachedId(" + memcachedId );

        try {
            mcClient.store( memcachedId, record );
        } catch ( Exception ex ) {
            //log.warn ( "FAULT " + Fault.CACHE_FAULT + ":" +
            //           Fault.getMessage( Fault.CACHE_FAULT ) +
            //           ":" + ex.toString() );
        }
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

            return result;

        } catch ( Exception e ) {
            log.warn( "marshall(): exception: " + e.toString() );
            throw FaultFactory.newInstance( Fault.MARSHAL );
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

            return dxfResult;
            
        } catch ( Exception e ) {
            log.warn( "unmarshall(): exception: " + e.toString() );
            throw FaultFactory.newInstance( Fault.MARSHAL );
        }
    }
}
