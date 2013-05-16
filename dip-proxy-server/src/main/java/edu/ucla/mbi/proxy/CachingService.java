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

    private McClient mcClient = null;
    private RemoteNativeService rns = null;

    public CachingService( WSContext wsContext, String provider ) 
        throws ServerFault {
        
        rns = new RemoteNativeService ( wsContext, provider );
        
        if( mcClient == null ) {
            mcClient = rns.getWsContext().getMcClient();
        }
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
        log.info( " ramCacheOn=" + rns.getRsc().isRamCacheOn() );
        log.info( " dbCacheon=" + rns.getRsc().isDbCacheOn() );
        
        NativeRecord nativeRecord = null;
        NativeRecord expiredRecord = null;
        NativeRecord remoteRecord = null;
        
        ServerFault serverFault = null;

        Date currentTime = Calendar.getInstance().getTime();
        
        String memcachedId = "NATIVE_" + provider + "_" + service +
                             "_" + ns + "_" + ac;

        //*** retrieve from memcached
        if( rns.getRsc().isRamCacheOn() ) {
            NativeRecord memcachedRec = null;
            try {
                memcachedRec = (NativeRecord)mcClient.fetch( memcachedId );
            } catch ( Exception ex ) {
                log.warn ( "FAULT: CACHE_FAULT: " + ex.toString() );
                serverFault = ServerFaultFactory.newInstance( Fault.CACHE_FAULT ); 
            }

            log.info( "getNative: memcachedRec=" + memcachedRec );

            if( memcachedRec != null ) {
                nativeRecord = memcachedRec;
            }
        }

        //*** retrieve from local database
        if ( nativeRecord == null && rns.getRsc().isDbCacheOn() ) {

            NativeRecord cacheRecord = null;

            cacheRecord = rns.getWsContext().getDipProxyDAO()
                .findNativeRecord( provider, service, ns, ac );

            if ( cacheRecord != null ) { // local record present

                String natXml = cacheRecord.getNativeXml();
                
                if( natXml == null || natXml.isEmpty() ) {

                    rns.getWsContext().getDipProxyDAO()
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
            if( rns.getRsc().isDbCacheOn() ) {               
                log.info( "db create nativeR. " );
                rns.getWsContext().getDipProxyDAO()
                    .createNativeRecord( nativeRecord );                
            }

            //*** memcached store
            if( rns.getRsc().isRamCacheOn() ) {
                memcachedStore ( memcachedId, nativeRecord );
            }

            return nativeRecord;

        }  else if( expiredRecord != null ){

            if( rns.getRsc().isDbCacheOn() ){
                log.info( "db create expiredR. " );
                rns.getWsContext().getDipProxyDAO()
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

        //} catch( ProxyFault fault){
        //    throw ServerFaultFactory
        //        .newInstance( fault.getFaultInfo().getFaultCode() );            
        //}
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

        

        //*** retrieve from memcached
        if( rns.getRsc().isRamCacheOn() ){
            DxfRecord memcachedRec = null;
            try {
                memcachedRec = (DxfRecord)mcClient.fetch( memcachedId );
            } catch ( Exception ex ) {
                serverFault = ServerFaultFactory.newInstance( Fault.CACHE_FAULT );
            }

            log.info( "getDxf: memcachedRec=" + memcachedRec );

            if( memcachedRec !=  null ) {
                dxfRecord = memcachedRec;
            }
        }

        //*** retrieve from local database 
        if( dxfRecord == null && rns.getRsc().isDbCacheOn() ){
            
            DxfRecord cacheDxfRecord = null;

            cacheDxfRecord = rns.getWsContext().getDipProxyDAO()
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
                    rns.getWsContext().getDipProxyDAO()
                        .deleteDxfRecord ( dxfRecord );
                }
            }
        }

        //*** retrieve from native, here dxfRecord==null|expired              
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

            if( rns.getRsc().isDbCacheOn() ) {
                rns.getWsContext().getDipProxyDAO()
                    .createDxfRecord ( dxfRecord );
            }
            
            if( rns.getRsc().isRamCacheOn() ) {
                memcachedStore ( memcachedId, dxfRecord );
            }

            return dxfRecord;

        } else if( expiredDxf != null ){
            
            if( rns.getRsc().isDbCacheOn() ){
                rns.getWsContext().getDipProxyDAO()
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
        
        ProxyDxfTransformer pdt = new ProxyDxfTransformer( rns.getWsContext() );

        try{

        dxfResult = pdt.buildDxf( nativeXml, ns,ac, detail,
                                  provider, service );
                

        } catch( ProxyFault fault){
            throw ServerFaultFactory
                .newInstance( fault.getFaultInfo().getFaultCode() );            
        }
        
        if ( isDxfDatasetValid( dxfResult ) ){
            
            String dxfString = null;
            
            //*** mashall DatasetType object into a string representation
            dxfString = marshall( dxfResult );
               
            dxfRecord = new DxfRecord( provider, service, ns,
                                       ac, detail );                
            
            dxfRecord.setDxf( dxfString );
            dxfRecord.resetExpireTime ( nativeRecord.getQueryTime(),
                                        rns.getRsc().getTtl() );                

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
            mcClient.store( memcachedId, record );
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
