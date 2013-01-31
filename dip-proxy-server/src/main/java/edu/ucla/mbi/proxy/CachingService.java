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
        

        DxfRecord dxfRecord = null;
        boolean dxfExpired = false;
        DxfRecord expiredDxf = null;
        DatasetType dxfRslt = null;
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
                dxfRslt = unmarshall( memcachedRec.getDxf() );
                return dxfRslt;
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
                    dxfRslt = unmarshall( dxfRecord.getDxf() );
                } catch ( ProxyFault fault ) {
                    proxyFault = fault;
                }

                if( dxfRslt != null ) {
                    if ( dxfRslt.getNode().isEmpty()
                         || dxfRslt.getNode().get(0).getAc().equals("") ) {

                        log.info( "CachingService: " +
                                  "dxf record is empty or there is no ac.");
                        try {
                            DipProxyDAO.getDxfRecordDAO().delete( dxfRecord );
                        } catch ( DAOException ex ) {
                            proxyFault = FaultFactory.newInstance( 
                                                        Fault.TRANSACTION );
                        }        

                        dxfRecord = null;

                    } else {
                        Date expirationTime = dxfRecord.getExpireTime();
                        Date currentTime = Calendar.getInstance().getTime();

                        log.info( "CachingService: dxf record CT=" +
                                  dxfRecord.getCreateTime() + " ET= " +
                                  expirationTime );

                        if ( currentTime.after( expirationTime ) ) {
                            dxfExpired = true;
                            expiredDxf = dxfRecord;
                            expiredResult = dxfRslt;
                        } else {
                            //*** return valid record from dbCache
                            if( rsc.isRamCacheOn() ) {
                                memcachedStore ( memcachedId, dxfRecord );
                            }
                            //*** return valid result from dbCache
                            return dxfRslt;
                        }
                    }
                }
            }
        }

        //*** valid dxf record not available when here ( null or expired )

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

            if ( currentTime.after( nr.getExpireTime() ) 
                 && dxfExpired ) {

                if( nr.getExpireTime().after( expiredDxf.getExpireTime() ) ) {
                    //*** return most recently expired record
                    return expiredResult;
                } 

                dxfExpired = true;
            } 

            //*** build dxf/expired dxf from nativeRecord
            nativeXml = nr.getNativeXml();
            RemoteServer rs = selectRemoteServer( rsc, service );
            DatasetType dxfResult = null;

            try {
                dxfResult = rs.buildDxf( nativeXml, ns, ac,
                                         detail, provider, service );

            } catch( ProxyFault fault ) {
                log.warn( "getDxf(): transform error for service " + service +
                          " and ac " + ac + " exception: "+ fault.toString());
                proxyFault = fault;
            }

            if ( !dxfResult.getNode().isEmpty()
                 && !dxfResult.getNode().get(0).getAc().equals("") ) {

                //*** mashall DatasetType object into a string representation
                String dxfString = null;

                if ( dxfRecord == null ) {
                    dxfRecord = new DxfRecord( provider, service,
                                               ns, ac, detail );

                    Date queryTime = dxfRecord.getCreateTime();  // primary query
                    Calendar qCal = Calendar.getInstance();
                    qCal.setTime( queryTime );
                    qCal.add( Calendar.SECOND, rsc.getTtl() );

                    dxfRecord.setExpireTime( qCal.getTime() );
                }

                try {
                    dxfString = marshall( dxfResult );
                } catch ( ProxyFault fault ) {
                    proxyFault = fault;
                    dxfRecord = null;
                }

                if ( dxfRecord != null ) {
                    if( dxfExpired ) {
                        expiredDxf = dxfRecord;
                        expiredResult = dxfResult;
                    } else {
                        //*** return a valid dxfReslut
                        if( rsc.isRamCacheOn() ) {
                            memcachedStore ( memcachedId, dxfRecord );
                        }

                        if( rsc.isDbCacheOn() ) {
                            dxfRecord.setDxf( dxfString );
                            dxfRecord.resetExpireTime( nr.getQueryTime(), rsc.getTtl() );
                            DipProxyDAO.getDxfRecordDAO().create( dxfRecord );
                        }
                        
                        return dxfResult;
                    }
                }
            }
        } 

        if( expiredResult != null ) {
            return expiredResult;
        } else if ( proxyFault != null ) {
            throw proxyFault;
        } else {
            return null;
        }
    }

    private RemoteServer selectRemoteServer( RemoteServerContext rsc,
                                             String service ) {

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
