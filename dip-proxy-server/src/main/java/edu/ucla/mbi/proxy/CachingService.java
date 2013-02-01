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

public class CachingService extends CachingNativeService {

    private Log log = LogFactory.getLog( CachingService.class );
    
    private static DxfRecordDAO dxfDAO = DipProxyDAO.getDxfRecordDAO();

    public CachingService( String provider, Router router, 
                           RemoteServerContext rsc ) {

        super( provider, router, rsc );
    }
    
    private CachingService() { }

    //--------------------------------------------------------------------------

    public DatasetType getDxf( String provider, String service, String ns,
                               String ac, String detail 
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
                log.warn ( "FAULT " + Fault.CACHE_FAULT + 
                           ":" + Fault.getMessage( Fault.CACHE_FAULT ) + 
                           ":" + ex.toString() );
            }
        
            log.info( "getDxf: memcachedRec=" + memcachedRec );

            if( memcachedRec !=  null ) {
                //*** return a valid result from memcached
                dxfResult = unmarshall( memcachedRec.getDxf() );
                return dxfResult;
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
                
            if ( dxfRecord != null ) {
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

        //*** retrieve from native      
        
        NativeRecord nr = null;
        String nativeXml = null;
 
        try { 
            nr = getNative( provider, service, ns, ac );
        } catch ( ProxyFault fault ) {
            proxyFault = fault;
        } 

        if( nr != null ) {
            nativeXml = nr.getNativeXml();
            Date currentTime = Calendar.getInstance().getTime();

            if ( currentTime.after( nr.getExpireTime() ) ) {
                //*** nr is expired

                if( dxfExpired 
                    && nr.getExpireTime().after( 
                        expiredDxf.getExpireTime() ) ) {
                        
                    //*** return expired coming from dbCache
                    return expiredResult;
                }
                dxfExpired = true;
            } else {
                dxfExpired = false;
            } 
        
            //*** get dxf from remote proxy or remote native using valid/expired nr
            try {
                dxfResult = getDxfFromRemote ( nativeXml, provider, 
                                               service, ns, ac, detail );
            } catch ( ProxyFault fault ) {
                proxyFault = fault;
            }
        }
            
        if ( dxfResult != null ) {

            //*** mashall DatasetType object into a string representation
            try {
                dxfString = marshall( dxfResult );
            } catch ( ProxyFault fault ) {
                proxyFault = fault;
            }
    
            if ( dxfRecord == null ) {
                dxfRecord = new DxfRecord( provider, service,
                                           ns, ac, detail );
            }

            dxfRecord.setDxf( dxfString );
            
            if( dxfExpired ) {
                dxfRecord.resetExpireTime ( nr.getQueryTime(),
                                            rsc.getTtl() );
            
                if( rsc.isDbCacheOn() ) {
                    //*** using most recently expired update expired
                    DipProxyDAO.getDxfRecordDAO().create( dxfRecord );
                }
                //*** return expired dxf coming from remote
                return dxfResult;

            } else {
                dxfRecord.resetExpireTime ( rsc.getTtl() );

                if( rsc.isRamCacheOn() ) {
                    memcachedStore ( memcachedId, dxfRecord );
                }

                if( rsc.isDbCacheOn() ) {
                    DipProxyDAO.getDxfRecordDAO().create( dxfRecord );
                }
                //*** return valid dxf coming from remote       
                return dxfResult;
            }
        }

        if( expiredResult != null ) {
            //*** return expired dxf coming from dbCache, here remote is null
            return expiredResult;
        } else if ( proxyFault != null ) {
            throw proxyFault;
        } else {
            return null;
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
