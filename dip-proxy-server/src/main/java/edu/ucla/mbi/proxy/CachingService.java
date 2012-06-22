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
        String natXml = null;
	
        boolean cacheExpired = true;
	
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
                    natXml = null;
                    cacheExpired = true; 
                } else {
                    natXml = cacheRecord.getNativeXml();
                    if( natXml != null ) {
                        cacheExpired = false;
                    }
                }
            }
        }
        
        NativeRecord remoteRec = null;
        
        if ( cacheExpired ) {  // NOTE: includes empty cache
            
            int retry = router.getMaxRetry();
            
            boolean remoteExpired = true;
            
            while ( retry > 0 && natXml == null && remoteExpired ) {
                
                // no valid local copy - try remote source(s)
                
                RemoteServer rs = 
                    selectNextRemoteServer( provider, service, ns, ac );
                
                log.info( " selected rs=" + rs );
                log.info( " retries left=" + retry );
                retry--;
                
                try{
                    remoteRec = rs.getNative( provider, service, ns, ac, 
                                              rsc.getTimeout() );
                    log.info( "getNative: remoteRec=" + remoteRec );
                }catch(ProxyFault fault){
                    log.warn("RemoteServer getNative() fault: " 
                             + fault.getFaultInfo().getMessage());                    
                    throw fault;
                }
		            
                Date queryTime = remoteRec.getCreateTime();  // primary query
                
                Calendar qCal = Calendar.getInstance();
                qCal.setTime( queryTime );
                qCal.add( Calendar.SECOND, rsc.getTtl() );
		
                if( currentTime.after( qCal.getTime() ) ) {
                    remoteExpired = true;
                    log.info( "getNative: remoteExpired=true. " );
                } else {
                    remoteExpired = false;
                    natXml = remoteRec.getNativeXml();
                }
                
                if ( natXml == null ) { // remote site problem
                    // NOTE: should also drop on exception remote exception ???
                    
                    this.setChanged(); // drop site from DHT
                    
                    DhtRouterMessage message =
                        new DhtRouterMessage( DhtRouterMessage.DELETE,
                                              remoteRec, rs );
                    
                    this.notifyObservers( message ); 
                    this.clearChanged();
                }
            }
            
            if ( natXml == null || natXml.length() == 0 ) {

                // no remote site found - throw exception
                // NOTE: distinguish between no record vs error here ? 
                
                log.warn( "Get nativeXml null. " );
                throw FaultFactory.newInstance( Fault.NO_RECORD );
            }
        }

        // NOTE: cacheRecord and/or remoteRecord present if here
        //------------------------------------------------------
        
        if ( rsc.isCacheOn() ) { // update/store in the local cache
            
            if ( cacheRecord == null ) { // create new local record
                
                log.info( "  CachingService: creating cache record" );
		
                cacheRecord = new NativeRecord( provider, service, ns, ac );
                cacheRecord.setNativeXml( remoteRec.getNativeXml() );
                
		        // NOTE: remoteRecord must specify time of the primary 
		        //       source query
		
		        Date remoteQueryTime = remoteRec.getCreateTime();
		        cacheRecord.resetExpireTime( remoteQueryTime, rsc.getTtl() ); 

		        log.info( "  CachingService: rqt=" + remoteQueryTime );
		        log.info( "  CachingService: lqt=" + cacheRecord.getQueryTime() );

            } else {
                
                if (  remoteRec != null ) {  // update cache record to remote
                    
                    log.info( "  CachingService: updating cache record" );
                    
                    // NOTE: cached records so use getQueryTime()
				
                    cacheRecord.setNativeXml( remoteRec.getNativeXml() );
                    
                    // NOTE: remoteRecord must specify time of the primary
                    //       source query

                    Date remoteQueryTime = remoteRec.getCreateTime();
                    cacheRecord.resetExpireTime( remoteQueryTime, rsc.getTtl() );
                    
                } else {
                    
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
                                      // rsc.getProxyProto() );
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

        if ( rsc.isCacheOn() ) {
            
            boolean dxf_expired = false;
            
            try {
                
                // get cached copy of dxf record
                // ------------------------------
                
                dxfRecord = DipProxyDAO.getDxfRecordDAO()
                                .find( provider, service, ns, ac, detail );
                
                // check the expiration date
                // -------------------------
                
                if ( dxfRecord != null ) {
                    
                    Date expirationTime = dxfRecord.getExpireTime();
                    Date currentTime = Calendar.getInstance().getTime();
                    
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

        // valid dxf record not available when here
        // -----------------------------------------
        
        NativeRecord nr = getNative( provider, service, ns, ac );
        NativeRecord nrN = DipProxyDAO.getNativeRecordDAO().find( nr.getId() );
        
        String nativeXml = null;
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

        // transform nativeXML to DXF
        // ---------------------------
        
        // NOTE: parametrize getTransformer() with detail here ?
        
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
            
            NativeRecord faultyRecord = DipProxyDAO.getNativeRecordDAO()
                                            .find( provider, service, ns, ac );

            faultyRecord.setNativeXml( "" );
            
            Calendar expCal = Calendar.getInstance();
            faultyRecord.setExpireTime( expCal.getTime() );
            DipProxyDAO.getNativeRecordDAO().create( faultyRecord );
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
            throw FaultFactory.newInstance( Fault.NO_RECORD );
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
                dxfRecord.resetExpireTime( rsc.getTtl() );

                
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

    private String marshall( DatasetType record ) 
        throws ProxyFault {

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
