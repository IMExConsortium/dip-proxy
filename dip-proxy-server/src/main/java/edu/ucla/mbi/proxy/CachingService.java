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
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.cache.*;

public class CachingService {

    private Log log = LogFactory.getLog( CachingService.class );

    private WSContext wsContext = null;

    //public CachingService( WSContext wsContext, String provider ) 
    public CachingService( WSContext wsContext ) throws ServerFault {
        this.wsContext = wsContext;
    }
    
    public CachingService() { }

    //--------------------------------------------------------------------------

    public NativeRecord getNative( String provider, 
                                   String service, 
                                   String ns,
                                   String ac 
                                   ) throws ServerFault {        
        
        String id = provider + "_" + service + "_" + ns + "_" + ac;

        log.info( "getNative(provider=" + provider + ")" );
        log.info( " ramCacheOn=" 
                  + wsContext.isRamCacheOnForProvider( provider ) );
        log.info( " dbCacheon=" 
                  + wsContext.isDbCacheOnForProvider( provider ) );
        
        NativeRecord nativeRecord = null;
        NativeRecord expiredRecord = null;
        NativeRecord remoteRecord = null;
        
        ServerFault serverFault = null;

        Date currentTime = Calendar.getInstance().getTime();
        
        String memcachedId = "NATIVE_" + provider + "_" + service +
                             "_" + ns + "_" + ac;

        // try to retrieve from memcached
        
        if( wsContext.isRamCacheOnForProvider( provider ) ) {
            NativeRecord memcachedRec = null;
            try {
                memcachedRec = 
                    (NativeRecord)wsContext.getMcClient().fetch( memcachedId );
            } catch ( Exception ex ) {
                log.warn ( "FAULT: CACHE_FAULT: " + ex.toString() );
                serverFault = ServerFaultFactory.newInstance(Fault.CACHE_FAULT); 
            }

            log.info( "getNative: memcachedRec=" + memcachedRec );

            if( memcachedRec != null ) {
                nativeRecord = memcachedRec;
            }
        }

        // try retrieve from local database

        if ( nativeRecord == null 
             && wsContext.isDbCacheOnForProvider( provider ) ) {

            NativeRecord cacheRecord = null;

            cacheRecord = wsContext.getDipProxyDAO()
                .findNativeRecord( provider, service, ns, ac );

            if ( cacheRecord != null ) { // local record present

                String natXml = cacheRecord.getNativeXml();
                
                if( natXml == null || natXml.isEmpty() ) {

                    wsContext.getDipProxyDAO()
                        .deleteNativeRecord( cacheRecord );
 
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
                        log.info( "getNativeRecord: expiredRecord=" 
                                  + expiredRecord );
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
            RemoteNativeService rns = 
                new RemoteNativeService ( wsContext, provider );

            remoteRecord = rns.getNativeFromRemote ( provider, service, ns, ac );
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
            if( wsContext.isDbCacheOnForProvider( provider ) ) {               
                log.info( "db create nativeR. " );
                wsContext.getDipProxyDAO()
                    .createNativeRecord( nativeRecord );
            }

            //*** memcached store
            if( wsContext.isRamCacheOnForProvider( provider ) ) {
                memcachedStore ( memcachedId, nativeRecord );
            }

            return nativeRecord;

        }  else if( expiredRecord != null ){

            if( wsContext.isDbCacheOnForProvider( provider ) ){
                log.info( "db create expiredR. " );
                wsContext.getDipProxyDAO()
                    .createNativeRecord( expiredRecord );
            }
            
            return expiredRecord;

        } else if ( serverFault != null ) {

            log.warn( "getNative: throw a serverFault. " );
            throw serverFault;

        } else {
            log.info( "getNative: return a null. " );
            return null;
        }
    }

    //--------------------------------------------------------------------------

    public DatasetType getDatasetType( DxfRecord dxfRecord )
        throws ServerFault{

        if( dxfRecord != null && dxfRecord.getDxf() != null 
            && !dxfRecord.getDxf().isEmpty() ) {

            return unmarshall( dxfRecord.getDxf() );
        }
        return null;
    }
    
    //--------------------------------------------------------------------------

    public  DatasetType getDatasetType( String provider, String service,
                                        String ns, String ac, String detail
                                        ) throws ServerFault {
        
        DxfRecord dxfRecord = getDxfRecord( provider,service, ns,ac, detail);
        return getDatasetType( dxfRecord );
    }
    
    //--------------------------------------------------------------------------
    
    public DxfRecord getDxfRecord( String provider,
                                   String service,
                                   String ns,
                                   String ac,
                                   String detail
                                   ) throws ServerFault {
        
        DxfRecord dxfRecord = null;
        DxfRecord expiredDxf = null;

        ServerFault serverFault = null;

        String memcachedId = "DXF_" + provider + "_" + service + "_" + ns +
                             "_" + ac + "_" + detail;

        Date currentTime = Calendar.getInstance().getTime();

        // try retrieve from memcached
        
        if( wsContext.isRamCacheOnForProvider( provider ) ){
            DxfRecord memcachedRec = null;
            try {
                memcachedRec = 
                    (DxfRecord)wsContext.getMcClient().fetch( memcachedId );
            } catch ( Exception ex ) {
                serverFault = ServerFaultFactory.newInstance( Fault.CACHE_FAULT );
            }

            log.info( "getDxf: memcachedRec=" + memcachedRec );

            if( memcachedRec !=  null ) {
                dxfRecord = memcachedRec;
            }
        }

        // try to retrieve from local database 
        
        if( dxfRecord == null 
            && wsContext.isDbCacheOnForProvider( provider ) ){
            
            DxfRecord cacheDxfRecord = null;

            cacheDxfRecord = wsContext.getDipProxyDAO()
                .findDxfRecord ( provider, service, ns, ac, detail );

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
                    wsContext.getDipProxyDAO()
                        .deleteDxfRecord ( dxfRecord );
                }
            }
        }

        // try retrieve from native; if here dxfRecord==null|expired              
        //----------------------------------------------------------
        
        if( dxfRecord == null ) { 
            
            NativeRecord nativeRecord = null;
            String nativeXml = null;
            
            try { 
                nativeRecord  = getNative( provider, service, ns, ac );
            } catch ( ServerFault fault ) {
                log.warn( "getDxf: getNative fault. " ); 
                serverFault = fault;
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
                } catch ( ServerFault fault ) {
                    log.warn( "getDxf: buildDxfRecord. " );
                    serverFault = fault;
                }
            }    
        }
        
        if( dxfRecord != null ) {

            if( wsContext.isDbCacheOnForProvider( provider ) ) {
                wsContext.getDipProxyDAO()
                    .createDxfRecord ( dxfRecord );
            }
            
            if( wsContext.isRamCacheOnForProvider( provider ) ) {
                memcachedStore ( memcachedId, dxfRecord );
            }

            return dxfRecord;

        } else if( expiredDxf != null ){
            
            if( wsContext.isDbCacheOnForProvider( provider ) ){
                wsContext.getDipProxyDAO()
                    .createDxfRecord ( expiredDxf );
            }
            
            return expiredDxf;

        } else if ( serverFault != null ) {
            log.warn( "getDxf: throw a serverFault. " );
            throw serverFault;
        } else {
            log.info( "getDxf: return a null. " );
            return null;
        }
        
    }
    
    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------

    private DxfRecord buildDxfRecord( NativeRecord nativeRecord,
                                      String detail ) 
        throws ServerFault{
        
        DxfRecord dxfRecord = null;
        
        DatasetType dxfResult = null;
        ServerFault serverFault = null;

        String provider = nativeRecord.getProvider();
        String service = nativeRecord.getService();
        String ns = nativeRecord.getNs();
        String ac = nativeRecord.getAc();
        String nativeXml = nativeRecord.getNativeXml();
        
        try{
            
            dxfResult = wsContext.getDxfTransformer( provider )
                .buildDxf( nativeXml, ns, ac, detail, service );
            
        } catch( ServerFault fault){
            throw fault;
        }
        
        if ( isDxfDatasetValid( dxfResult ) ){
            
            String dxfString = null;
            
            //  mashall DatasetType object into a string representation
            
            dxfString = marshall( dxfResult );
            
            dxfRecord = new DxfRecord( provider, service, ns,
                                       ac, detail );                
            
            dxfRecord.setDxf( dxfString );
            dxfRecord.resetExpireTime ( nativeRecord.getQueryTime(),
                                        wsContext.getTtlForProvider( provider ) );                

        }
 
        return dxfRecord;
    }


    //-------------------------------------------------------------------------- 

    private boolean isDxfValid( String dxfString ) throws ServerFault { 

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
            wsContext.getMcClient().store( memcachedId, record );
        } catch ( Exception ex ) {
            log.warn ( "memcachedStore get exception: " + ex.toString() );
        }
    }

    private String marshall( DatasetType record ) throws ServerFault {

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
            throw ServerFaultFactory.newInstance( Fault.MARSHAL );
        }
    }

    private DatasetType unmarshall( String dxfString ) 
        throws ServerFault {
        
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
            throw ServerFaultFactory.newInstance( Fault.MARSHAL );
        }
    }
}
