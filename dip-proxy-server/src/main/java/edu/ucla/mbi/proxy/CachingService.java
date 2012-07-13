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

public class CachingService extends Observable {

    protected String provider;
    private RemoteServerContext rsc;
    private Router router;

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

        Log log = LogFactory.getLog( CachingService.class );
        Date currentTime = Calendar.getInstance().getTime();
        
        log.info( "getNative(provider=" + provider + ")" );
        log.info( "         (router=" + router + ")" );
        log.info( "         (router.rsc="
                  + router.getRemoteServerContext().getProvider() + ")" );
        log.info( " cache on=" + rsc.isCacheOn() );

        NativeRecord cacheRecord = null;
        boolean cacheExpired = false;
        
        NativeRecord remoteRec = null;
        boolean remoteExpired = false;
        
        String natXml = null;
	
        if ( rsc.isCacheOn() ) { // try retrieve from local cache

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
                        log.info( "Native record: return from local cache. ");
                        //*** return valid record from local cache  
                        return cacheRecord;
                    }
                }
            }
        }
        
        if ( cacheRecord == null || cacheExpired ) {  
            // NOTE: includes empty cache and expired cache
            
            int retry = router.getMaxRetry();            
            NativeRecord expiredRemoteRec = null;

            while ( retry > 0 && natXml == null ) {
                
                // no valid local copy - try remote source(s)
                
                RemoteServer rs = 
                    selectNextRemoteServer( provider, service, ns, ac );
                
                log.info( " selected rs=" + rs );
                log.info( " retries left=" + retry );
                retry--;
                
                try {
                    remoteRec = rs.getNative( provider, service, ns, ac, 
                                              rsc.getTimeout() );
                    log.info( "getNative: remoteRec=" + remoteRec );
                } catch( ProxyFault fault ) {
                    log.warn("getNative: RemoteServer getNative() fault: " 
                             + fault.getFaultInfo().getMessage());                    

                    if( fault.getFaultInfo().getFaultCode() == 5 ) {
                       log.info( "getNative: faultCode=5 throw NO_RECORD fault. ");
                       throw fault; // NO_RECORD fault
                    }
                }
		      
                if( remoteRec != null ) {      
                    Date queryTime = remoteRec.getCreateTime();  // primary query
                
                    Calendar qCal = Calendar.getInstance();
                    qCal.setTime( queryTime );
                    qCal.add( Calendar.SECOND, rsc.getTtl() );
		
                    boolean messageDelete = false;

                    if( currentTime.after( qCal.getTime() ) ) {
                        log.info( "getNative: remoteExpired=true. " );
                        String expiredNatXml = remoteRec.getNativeXml();
                        
                        if( expiredNatXml != null && expiredNatXml.length() > 0 ) {
                            if( expiredRemoteRec == null ) {
                                expiredRemoteRec = remoteRec;
                                log.info( "getNative: got a remote expiredRec." );
                            } else {
                                if( expiredRemoteRec.getExpireTime().after( 
                                        remoteRec.getExpireTime() ) ) 
                                {
                                    //*** using more recentlly expired record
                                    expiredRemoteRec = remoteRec; 
                                }
                            }
                        } else {
                            messageDelete = true;
                        }
                    } else {
                        natXml = remoteRec.getNativeXml();
    
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
                                                      remoteRec, rs );

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
                        //*** remoteRec is a expired record
                        remoteRec = expiredRemoteRec;
                        natXml = remoteRec.getNativeXml();
                        remoteExpired = true;
                    }
                } else {
                    remoteRec = null;
                } 
            }
        }

        // NOTE: cacheRecord and/or remoteRecord present if here
        //------------------------------------------------------
        
        if ( rsc.isCacheOn() ) { // update/store in the local cache
            
            if ( cacheRecord == null ) {
                //in this case, remoteRec is not null    
                    
                //*** create new local record based on remoteRec
                
                log.info( "  CachingService: creating cache record" );
		
                cacheRecord = new NativeRecord( provider, service, ns, ac );
                cacheRecord.setNativeXml( remoteRec.getNativeXml() );
                
		        // NOTE: remoteRecord must specify time of the primary 
		        //       source query
		
                Date remoteQueryTime;
                if( remoteExpired ) {
		            remoteQueryTime = remoteRec.getQueryTime();
                } else {
                    remoteQueryTime = remoteRec.getCreateTime();
                }

		        cacheRecord.resetExpireTime( remoteQueryTime, rsc.getTtl() ); 

		        log.info( "  CachingService: rqt=" + remoteQueryTime );
		        log.info( "  CachingService: lqt=" + cacheRecord.getQueryTime() );
            } else if ( cacheExpired ) {
                //*** local cache is expired     
                if ( !remoteExpired && remoteRec != null ) {  
                    //*** update cache record from valid remote record
                    
                    log.info( "  CachingService: updating cache record" );
                    
                    cacheRecord.setNativeXml( remoteRec.getNativeXml() );
                    
                    // NOTE: remoteRecord must specify time of the primary
                    //       source query

                    Date remoteCreateTime = remoteRec.getCreateTime();
                    cacheRecord.resetExpireTime( remoteCreateTime, rsc.getTtl() );
                    
                } else if( remoteExpired && remoteRec != null 
                            && cacheRecord.getExpireTime().after( 
                                    remoteRec.getExpireTime() ) ) 
                {
                    //*** update caceh record from expired remote record
                    cacheRecord.setNativeXml( remoteRec.getNativeXml() );

                    // NOTE: remoteRecord must specify time of the primary
                    //       source query

                    Date remoteQueryTime = remoteRec.getQueryTime();
                    cacheRecord.resetExpireTime( remoteQueryTime, rsc.getTtl() );            
                } else {                   
                    //*** reset expired time for the expired cache record   
                    log.info( "  CachingService: cache record: updating ttl" );
                    Date queryTime = cacheRecord.getQueryTime(); // primary query
                    cacheRecord.resetExpireTime( queryTime, rsc.getTtl() );
                }
            }
            
            DipProxyDAO.getNativeRecordDAO()
                .create( cacheRecord ); // store/update native record locally

            if ( rsc.getDebugLevel() == 1 ) {
                log.info( " dropping native record = " + cacheRecord );
                DipProxyDAO.getNativeRecordDAO()
                    .delete( cacheRecord );
            }
            
            // notify interested parties (router) about new record
            // ----------------------------------------------------

            log.info( "  notify observers: new record" );
            this.setChanged();
            
            DhtRouterMessage message =
                new DhtRouterMessage( DhtRouterMessage.UPDATE, cacheRecord,
                                      null ); // self
            this.notifyObservers( message );
            this.clearChanged();
            
            return cacheRecord;
        } else {	    
            // caching off - return remoteRecord
            return remoteRec;
        }
    }
    
    //--------------------------------------------------------------------------

    public DatasetType getDxf( String provider, String service, String ns,
                               String ac, String detail 
                               ) throws ProxyFault 
    {
        Log log = LogFactory.getLog( CachingService.class );
        log.info( "getDxf(prv=" + provider + " srv=" + service + " det="
                  + detail + ")" );
        
        log.info( " cache on=" + rsc.isCacheOn() );

        if ( rsc == null ) {
            log.warn("remote server is null.");
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        DxfRecord dxfRecord = null;
        Date currentTime = Calendar.getInstance().getTime();
        boolean dxf_expired = false;

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
                    } else {
                        DatasetType dxfRslt = unmarshall( dxfRecord.getDxf() );
                        
                        if ( rsc.getDebugLevel() == 1 ) {
                            log.info( " dropping dxf record = " + dxfRecord );
                            DipProxyDAO.getDxfRecordDAO().delete( dxfRecord );
                        }

                        if ( !dxfRslt.getNode().isEmpty() 
                             && !dxfRslt.getNode().get(0).getAc().equals("") ) {
                            return dxfRslt;
                        }else{
                            log.info( "CachingService: " +
                                      "dxf record is empty or there is no ac.");
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
        
        NativeRecord nr = getNative( provider, service, ns, ac );
        String nativeXml = null;
        
        /*
        //*** the following maybe is useless
        NativeRecord nrN = DipProxyDAO.getNativeRecordDAO().find( nr.getId() );
        
        long startTime = System.currentTimeMillis();
        long waitMillis = WSContext.getWaitMillis();

        log.info( "getDxf: nativeRecordN nrN=" + nrN );
        
        while( nrN == null 
                && ( System.currentTimeMillis() - startTime < waitMillis ) ) 
        {
            nrN = DipProxyDAO.getNativeRecordDAO().find(nr.getId());
        }

        if ( nrN != null ) {
            nativeXml = nrN.getNativeXml();
            //log.info( "getDxf: nativeXml=" + nativeXml );
        }else{
            log.warn( "getDxf(): for service " + service +
                      " and ac " + ac + " cannot get native record" );
            throw FaultFactory.newInstance( Fault.NO_RECORD );
        }
        */

        if ( nr != null ) {
            nativeXml = nr.getNativeXml();
            //log.info( "getDxf: nativeXml=" + nativeXml );
        }else{
            log.warn( "getDxf(): for service " + service +
                      " and ac " + ac + " cannot get native record" );
            throw FaultFactory.newInstance( Fault.NO_RECORD );
        }
        
        //*** transform nativeXML to DXF
        // ---------------------------
        
        // NOTE: parametrize getTransformer() with detail here ?

        //**** check if nr is expired
        if ( currentTime.after( nr.getExpireTime() ) ) {
            if( dxf_expired ) {
                //*** nr is expired then return original expired dxf
                DatasetType dxfRslt = unmarshall( dxfRecord.getDxf() );

                if ( !dxfRslt.getNode().isEmpty()
                        && !dxfRslt.getNode().get(0).getAc().equals("") ) 
                {
                    return dxfRslt;
                }
            } else {
                //*** expired native cause expired dxf
                dxf_expired = true;
            } 
        } 
        
        RemoteServer rs = selectRemoteServer( rsc );
        
        DatasetType dxfResult = null;
        
        try {
            dxfResult = rs.buildDxf( nativeXml, ns, ac, 
                                     detail, service, rsc.getTransformer() );            
        } catch( ProxyFault se ) {
            log.info( "getDxf: get Exception: " + se.toString() );
            log.info( "getDxf: discarding native record" );
            
            // get cached copy of native record
            // ----------------------------------
            /*
            NativeRecord faultyRecord = DipProxyDAO.getNativeRecordDAO()
                                            .find( provider, service, ns, ac );

            faultyRecord.setNativeXml( "" );
            
            Calendar expCal = Calendar.getInstance();
            faultyRecord.setExpireTime( expCal.getTime() );
            DipProxyDAO.getNativeRecordDAO().create( faultyRecord );
            */

            /*
            try {
                DipProxyDAO.getNativeRecordDAO().delete( faultyRecord );
            } catch ( DAOException e ) {
                throw FaultFactory.newInstance( Fault.TRANSACTION );
            }
            */
            log.warn( "getDxf(): transform error for service " + service +
                      " and ac " + ac + " exception: "+ se.toString());
            throw FaultFactory.newInstance( Fault.TRANSFORM );
        }

        // build and store dxf record
        // ---------------------------

        if ( dxfResult.getNode().isEmpty() ) {
            log.warn( "getDxf(): dxf_record missing node: service=" +
                      service + " and ac=" + ac + ".");
            throw FaultFactory.newInstance( Fault.TRANSFORM );
        }
        
        if( dxfResult.getNode().get(0).getAc().equals("") ){
            log.warn("getDxf(): dxf_record missin ac: service=" + 
                     service + " and ac " + ac + ".");  
            
            throw FaultFactory.newInstance( Fault.TRANSFORM );            
        }
        
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
        return dxfResult;
    }
    
    private RemoteServer selectNextRemoteServer( String provider,
                                                 String service, 
                                                 String namespace, 
                                                 String accession ) {
        if ( rsc.isRemoteProxyOn() ) {
            
            Log log = LogFactory.getLog( CachingService.class );
            log.info( " selecting next proxy..." );
            
            // register as interested
            // ----------------------
            
            log.info( " adding observer..." );
            
            this.addObserver( router );
            return router.getNextProxyServer( service, 
                                              namespace, accession );
        }
        return router.getNativeServer();
    }
    
    private RemoteServer selectRemoteServer( RemoteServerContext rsc ) {

        if ( rsc.isRemoteProxyOn() ) {
            return router.getLastProxyServer();
        }
        return router.getNativeServer();
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
            Log log = LogFactory.getLog( CachingService.class );
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
            Log log = LogFactory.getLog( CachingService.class );
            log.warn( "unmarshall(): exception: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
    }
}
