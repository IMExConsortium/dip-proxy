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
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.cache.*;

public class CachingService {

    private Log log = LogFactory.getLog( CachingService.class );

    private WSContext wsContext = null;
    private RemoteNativeService rns = null;
    
    public CachingService() { }

    public void setWsContext(  WSContext context ){
        this.wsContext = context;
    }

    public void setRns( RemoteNativeService service ) {
        this.rns = service;
    }

    public NativeRecord getNative( String provider, 
                                   String service, 
                                   String ns,
                                   String ac 
                                   ) throws ServerFault {        
        
        String id = provider + "_" + service + "_" + ns + "_" + ac;

        log.info( "getNative(provider=" + provider + ")" );
        log.info( " ramCacheOn=" 
                  + wsContext.isRamCacheOn( provider ) );
        log.info( " dbCacheon=" 
                  + wsContext.isDbCacheOn( provider ) );
        
        NativeRecord nativeRecord = null;
        NativeRecord expiredRecord = null;
        NativeRecord remoteRecord = null;
        
        ServerFault serverFault = null;

        String memcachedId = "NATIVE_" + provider + "_" + service +
                             "_" + ns + "_" + ac;

        // try to retrieve from memcached
        //-------------------------------        

	log.info( " RamCacheOn: " + wsContext.isRamCacheOn( provider ));
	
        if( wsContext.isRamCacheOn( provider ) ) {
            NativeRecord memcachedRec = null;
            try {
                memcachedRec = 
                    (NativeRecord) wsContext.getMcClient().fetch( memcachedId );
            } catch ( Exception ex ) {
                log.warn ( "FAULT: CACHE_FAULT: " + ex.toString() );
                serverFault = ServerFaultFactory.newInstance(Fault.CACHE_FAULT); 
            }

            log.info( "getNative: memcachedRec=" + memcachedRec );

            if( memcachedRec != null ) {

                memcachedRec.resetExpireTime( memcachedRec.getQueryTime(),
                                              wsContext.getTtl( provider ) );
                
                if( isExpiredOfRecord( memcachedRec ) ) {
                    expiredRecord = memcachedRec;
                } else {
                    return memcachedRec;
                }
            }
        }

	log.info( " DbCacheOn: " + wsContext.isDbCacheOn( provider ));

        // try retrieve from local database
        //----------------------------------
        if ( nativeRecord == null 
             && wsContext.isDbCacheOn( provider ) ) {

            NativeRecord cacheRecord = null;
           
            cacheRecord = wsContext.getDipProxyDAO()
                .findNativeRecord( provider, service, ns, ac );

	    log.info( "Cached Native Record: " + cacheRecord);

            if ( cacheRecord != null ) { 
                // local record present
                //---------------------

                String natXml = cacheRecord.getNativeXml();
                
                if( natXml == null || natXml.isEmpty() ) {

                    wsContext.getDipProxyDAO()
                        .deleteNativeRecord( cacheRecord );
 
                    cacheRecord = null;
                } else {
		    
                    if( isExpiredOfRecord ( cacheRecord ) ) {
                        if( expiredRecord == null
                            || cacheRecord.getQueryTime()
                                .after( expiredRecord.getQueryTime() ) ){

                            expiredRecord = cacheRecord;
                        }
                    } else {
                        nativeRecord = cacheRecord;
                    }
                }
            }
        }

        // valid native record not available here ( null or expired ) 
        // retrieve from remote proxy server or native server  
        //--------------------------------------------------------------

        log.info( "Cached Native Record: " + nativeRecord);
        
        if( nativeRecord == null ){

            remoteRecord = rns.getNativeFromRemote( provider, service, ns, ac );
        }
        
	log.info( "Remote Native Record: " + remoteRecord);

        if( remoteRecord != null ) {
	   
            if( expiredRecord != null ){
                remoteRecord.setId( expiredRecord.getId() );
                remoteRecord.setCreateTime( expiredRecord.getCreateTime() );

                log.info( "getNativeRecord: remoteRecord set expiredR " +
                          "Id and remoteRecord=" + remoteRecord );
            }

            if( isExpiredOfRecord( remoteRecord ) ) {
                if( expiredRecord == null 
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
	    
	    nativeRecord.setAc( ac );  // LS: make sure record ac is the same as used in query
	   

            //*** dbCache update                           
            if( wsContext.isDbCacheOn( provider ) ) {               
                log.info( "db create nativeR. " );
                wsContext.getDipProxyDAO()
                    .createNativeRecord( nativeRecord );
            }

            //*** memcached store
            if( wsContext.isRamCacheOn( provider ) ) {
                memcachedStore ( memcachedId, nativeRecord );
            }

            return nativeRecord;

        }  else if( expiredRecord != null ){

            if( wsContext.isDbCacheOn( provider ) ){
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

    private boolean isExpiredOfRecord ( Record record ) { 

        Date currentTime = Calendar.getInstance().getTime();
        Date expirationTime = record.getExpireTime();

        log.info( " record: CT=" + record.getCreateTime() +
                  " ET=" + record.getExpireTime() );

        if ( currentTime.after( expirationTime ) ) {
            return true;
        } else {
            return false;
        }

    }

    public DatasetType getDatasetType( DxfRecord dxfRecord )
        throws ServerFault{

        if( dxfRecord != null && dxfRecord.getDxf() != null 
            && !dxfRecord.getDxf().isEmpty() ) {

            return unmarshall( dxfRecord.getDxf() );
        }
        return null;
    }
    
    public  DatasetType getDatasetType( String provider, String service,
                                        String ns, String ac, String detail
                                        ) throws ServerFault {
        
        DxfRecord dxfRecord = getDxfRecord( provider,service, ns,ac, detail);
        return getDatasetType( dxfRecord );
    }
    
    public DxfRecord getDxfRecord( String provider,
                                   String service,
                                   String ns,
                                   String ac,
                                   String detail
                                   ) throws ServerFault {
        

	log.info( "getDxfRecord(provider,service,ns,ac,detail) " + 
		  provider + " " + service + " " + ns + " " + ac + " " + detail );

        DxfRecord dxfRecord = null;
        DxfRecord expiredDxf = null;

        ServerFault serverFault = null;

        String memcachedId = "DXF_" + provider + "_" + service + "_" + ns +
                             "_" + ac + "_" + detail;

        // try retrieve from memcached
        //-----------------------------------

	log.info("RamCacheOn: " + wsContext.isRamCacheOn( provider));

        if( wsContext.isRamCacheOn( provider ) ){
            DxfRecord memcachedRec = null;
            try {
                memcachedRec = 
                    (DxfRecord)wsContext.getMcClient().fetch( memcachedId );
            } catch ( Exception ex ) {
                serverFault = ServerFaultFactory.newInstance(Fault.CACHE_FAULT);
            }

            log.info( "getDxf: memcachedRec=" + memcachedRec );

            if( memcachedRec !=  null ) {

                memcachedRec.resetExpireTime( memcachedRec.getQueryTime(),
                                              wsContext.getTtl( provider ) );

                if( isExpiredOfRecord( memcachedRec ) ) {
                    expiredDxf = memcachedRec;
                } else {
                    return memcachedRec;
                }

            }
        }

        // try to retrieve from local database 
        //-------------------------------------

	log.info("DbCacheOn: " + wsContext.isDbCacheOn( provider));

        if( dxfRecord == null 
            && wsContext.isDbCacheOn( provider ) ){
            
            DxfRecord cacheDxfRecord = null;

            cacheDxfRecord = wsContext.getDipProxyDAO()
                .findDxfRecord ( provider, service, ns, ac, detail );

	    log.info("Cached Dxf Record: " + cacheDxfRecord);

            if( cacheDxfRecord != null ) {
                
                if( isDxfValid ( cacheDxfRecord.getDxf() ) ) {
                    if( isExpiredOfRecord( cacheDxfRecord ) ) {
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
                log.warn( "getDxf: getNative fault for ac=" + ac + ". " ); 
                serverFault = fault;
            } 
            
            if( nativeRecord != null ){
                try{ 
                    DxfRecord dxfr =  buildDxfRecord( nativeRecord, detail );
                
                    if( dxfr != null ) {
                        if( expiredDxf != null) {
                            dxfr.setId( expiredDxf.getId() );
                        }
   
                        if( isExpiredOfRecord( nativeRecord ) ) {                    
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

            if( wsContext.isDbCacheOn( provider ) ) {
                wsContext.getDipProxyDAO()
                    .createDxfRecord ( dxfRecord );
            }
            
            if( wsContext.isRamCacheOn( provider ) ) {
                memcachedStore ( memcachedId, dxfRecord );
            }

            return dxfRecord;

        } else if( expiredDxf != null ){
            
            if( wsContext.isDbCacheOn( provider ) ){
                wsContext.getDipProxyDAO()
                    .createDxfRecord ( expiredDxf );
            }
            
            return expiredDxf;

        } else if ( serverFault != null ) {
            log.warn( "getDxf: throw a serverFault for ac=" + ac + ". " );
            throw serverFault;
        } else {
            log.info( "getDxf: return a null. " );
            return null;
        }
    }
    
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
            
            // mashall DatasetType object into a string representation
            //---------------------------------------------------------

            dxfString = marshall( dxfResult );
            
            dxfRecord = new DxfRecord( provider, service, ns,
                                       ac, detail );                
            
            dxfRecord.setDxf( dxfString );
            dxfRecord.resetExpireTime ( nativeRecord.getQueryTime(),
                                        wsContext.getTtl( provider ) );                

        }
 
        return dxfRecord;
    }

    private boolean isDxfValid( String dxfString ) throws ServerFault { 

        DatasetType dxfResult = unmarshall( dxfString );
        return isDxfDatasetValid( dxfResult );
    }

    private boolean isDxfDatasetValid( DatasetType  dxfDataset ) { 
                
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
