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
                       ":" + Fault.getMessage( Fault.CACHE_FAULT ) + 
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
                                           ":" + Fault.getMessage( Fault.CACHE_FAULT ) + 
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
                           ":" + Fault.getMessage( Fault.CACHE_FAULT ) + 
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
