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
    private static McClient mcClient = null;

    public CachingService( WSContext wsContext, String provider ) 
        throws ProxyFault {
 
        super( wsContext, provider );
        
        if( mcClient == null ) {
            mcClient = wsContext.getMcClient();
        }
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
        NativeRecord expiredRecord = null;
        NativeRecord remoteRecord = null;
        
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
                log.warn ( "FAULT: CACHE_FAULT: " + ex.toString() );
                proxyFault = FaultFactory.newInstance( Fault.CACHE_FAULT ); 
            }

            log.info( "getNative: memcachedRec=" + memcachedRec );

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
                        log.info( "getNativeRecord: put cacheRecord into "  +
                                  "expiredRecord."  );
                        expiredRecord = cacheRecord;
                        log.info( "getNativeRecord: expiredRecord=" + expiredRecord );
                    } else {
                        log.info( "getNative: return from dbCache." );
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
            if( expiredRecord != null ){
                remoteRecord.setId( expiredRecord.getId() );
                remoteRecord.setCreateTime( expiredRecord.getCreateTime() );

                log.info( "getNativeRecord: remoteRecord set expiredR " +
                          "Id and remoteRecord=" + remoteRecord );
            }

            if( currentTime.after( remoteRecord.getExpireTime() ) ) {
                //*** remote record is expired
                log.info( "getNative: remoteExpired=true. " );

                //*** select more recently expired record
                if(  expiredRecord == null 
                     || remoteRecord.getQueryTime()
                            .after( expiredRecord.getQueryTime() ) ){
                    
                    expiredRecord = remoteRecord;
                    log.info( "expiredR=" + expiredRecord );
                }
            } else {
                nativeRecord = remoteRecord;
            }
        }

        if( nativeRecord != null ){
            //*** dbCache update                           
            if( rsc.isDbCacheOn() ) {               
                log.info( "db create nativeR. " );
                DipProxyDAO.getNativeRecordDAO().create( nativeRecord );
            }

            //*** memcached store
            if( rsc.isRamCacheOn() ) {
                memcachedStore ( memcachedId, nativeRecord );
            }

            return nativeRecord;

        }  else if( expiredRecord != null ){

            if( rsc.isDbCacheOn() ){
                log.info( "db create expiredR. " );
                DipProxyDAO.getNativeRecordDAO().create( expiredRecord );
            }
            
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

    public  DatasetType getDatasetType( String provider, String service,
                                        String ns, String ac, String detail
                                        ) throws ProxyFault {
        
        DxfRecord dxfRecord = getDxfRecord( provider,service, ns,ac, detail);
        return getDatasetType( dxfRecord );
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

        ProxyFault proxyFault = null;

        String memcachedId = "DXF_" + provider + "_" + service + "_" + ns +
                             "_" + ac + "_" + detail;

        Date currentTime = Calendar.getInstance().getTime();
        

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
                dxfRecord = memcachedRec;
            }
        }

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
                
                    if( dxfr != null ) {
                        if( expiredDxf != null) {
                            dxfr.setId( expiredDxf.getId() );
                        }
    
                        if( currentTime.after( nativeRecord.getExpireTime() ) ) {
                        
                            //*** nativeRecord is expired
                            if( expiredDxf == null 
                                || nativeRecord.getQueryTime()
                                    .after ( expiredDxf.getQueryTime() ) ) {

                                expiredDxf = dxfr;
                            }
                        } else {
                            dxfRecord = dxfr;
                        }
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

        } else if( expiredDxf != null ){
            
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
        
        ProxyDxfTransformer pdt = new ProxyDxfTransformer( wsContext );

        dxfResult = pdt.buildDxf( nativeXml, ns,ac, detail,
                                  provider, service );
                
        
        if ( isDxfDatasetValid( dxfResult ) ){
            
            String dxfString = null;
            
            //*** mashall DatasetType object into a string representation
            dxfString = marshall( dxfResult );
               
            dxfRecord = new DxfRecord( provider, service, ns,
                                       ac, detail );                
            
            dxfRecord.setDxf( dxfString );
            dxfRecord.resetExpireTime ( nativeRecord.getQueryTime(),
                                        rsc.getTtl() );                

        }
 
        return dxfRecord;
    }


    //-------------------------------------------------------------------------- 

    private boolean isDxfValid( String dxfString ) throws ProxyFault { 

        DatasetType dxfResult = unmarshall( dxfString );
        
        return isDxfDatasetValid( dxfResult );
        
    }

    //--------------------------------------------------------------------------
    
    private boolean isDxfDatasetValid( DatasetType  dxfDataset ) { ;
                
        if( dxfDataset != null 
            && !dxfDataset.getNode().isEmpty()
            && !dxfDataset.getNode().get(0).getAc().equals("") ) {
            return true; 
        } 
        
        return false;
    }
    
    private void memcachedStore ( String memcachedId, Object record ) {

        log.info( "getNative: store cacheRecrod with " +
                  "memcachedId(" + memcachedId );

        try {
            mcClient.store( memcachedId, record );
        } catch ( Exception ex ) {
            log.warn ( "memcachedStore get exception: " + ex.toString() );
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
