package edu.ucla.mbi.proxy.ebi;

/*=======================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/#$
 * $Id$
 * Version: $Rev$
 *=========================================================================
 *
 * EbiCachingImpl - EBI Database access implemented 
 * through efetch SOAP
 *
 *  NOTE: Modify gen-src/axis2/ebi/resources/services.xml to use
 *  this instead of default EbiPublicSkeleton.
 *
 *====================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;

import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceException;
import edu.ucla.mbi.services.ServiceFault;
import edu.ucla.mbi.services.TimeStamp;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;

import edu.ucla.mbi.proxy.router.Router;

@WebService(endpointInterface = "edu.ucla.mbi.proxy.EbiProxyPort")
public class EbiCachingImpl implements EbiProxyPort {

    /*
     * Fetch uniprot from ebi dbfetch
     */
        
    public void getUniprot( String ns, String ac, String match, 
                            String detail, String format, 
                            String client, Integer depth,
                            Holder<XMLGregorianCalendar> timestamp,
                            Holder<DatasetType> dataset, 
                            Holder<String> nativerecord 
                            ) throws ProxyFault {
        
        String provider = "EBI";
        String service = "uniprot";
        
        Log log = LogFactory.getLog( EbiCachingImpl.class );
        log.info( "EbiCaching: getUniprot " + " NS=" + ns + " AC=" + ac
                  + " DT=" + detail );
        
        if ( !ns.equalsIgnoreCase( "uniprot" ) ) {
            log.info( "EbiCaching: forcing uniprot as ns" );
            ns = "uniprot";
        }
        
        if ( ac == null || ac.equals( "" ) ) {
            
            log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
        }
        
        if ( detail == null ) {
            detail = "stub";
        } else if ( detail.equalsIgnoreCase( "short" ) ||
                    detail.equalsIgnoreCase( "stub" ) ) {

            detail = "stub";

        } else if ( detail.equalsIgnoreCase( "base" ) ){
            detail = "base";

        } else {
            detail = "full";
        }

        try {
            Router router = 
                WSContext.getServerContext( provider ).createRouter();
            CachingService cachingSrv = 
                new CachingService( provider, router,
                                    WSContext.getServerContext( provider ) );
            
            if ( format == null || format.equals( "" )
                 || format.equalsIgnoreCase( "dxf" )
                 || format.equalsIgnoreCase( "both" ) ) {
                
                DatasetType result = 
                    cachingSrv.getDxf( provider, service, ns,
                                       ac, detail );
                if ( result != null ) {
                    dataset.value = result;
                } else {
                    log.info( "return dataset is null" );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

            if ( format != null
                 && (format.equalsIgnoreCase( "native" ) || format
                     .equalsIgnoreCase( "both" )) ) {
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service, ns, ac );

                if ( natRec != null && natRec.getNativeXml() != null
                     && natRec.getNativeXml().length() > 0 ) {
                    nativerecord.value = natRec.getNativeXml();
                    timestamp.value = 
                        TimeStamp.toXmlDate( natRec.getQueryTime() );
                } else {
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

        } catch ( ServiceException se ) {
            String message = se.getServiceFault().getMessage();
            log.warn( "EbiCachingImpl: ServiceException: message= " + message);

            ProxyFault fault = new ProxyFault(message, se.getServiceFault());
            throw fault;

        } catch ( Exception e ) {
            log.warn( "EbiCachingImpl: " + e.toString() );
            ServiceFault fault = new ServiceFault();
            fault.setMessage(e.toString());
            fault.setFaultCode(99);
            ProxyFault proxyFault = new ProxyFault(e.toString(), fault);
            throw proxyFault;
        }
    }

    public void getPicrList( String ns, String ac, String match, 
                             String detail, String format, 
                             String client, Integer depth,
                             Holder<XMLGregorianCalendar> timestamp,
                             Holder<DatasetType> dataset, 
                             Holder<String> nativerecord 
                             ) throws ProxyFault {

        String provider = "EBI";
        String service = "picr";

        Log log = LogFactory.getLog( EbiCachingImpl.class );
        log.info( "getPicrList " + "NS=" + ns + " AC=" + ac + " DT=" + detail );
        
        if ( ac == null || ac.equals( "" ) ) {

            log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
        }

        if( detail == null ) {
            detail = "base";
        } else if( detail.equalsIgnoreCase("short") ||
                   detail.equalsIgnoreCase("stub") ||
                   detail.equalsIgnoreCase("base") ) {

            detail = "base";

        } else {
            detail = "full";
        }

        try {
            Router router = WSContext.getServerContext( provider ).createRouter();
            CachingService cachingSrv = new CachingService( provider, router, WSContext.getServerContext( provider ) );

            if ( format == null || format.equals( "" )
                    || format.equalsIgnoreCase( "dxf" )
                    || format.equalsIgnoreCase( "both" ) ) {
                
                DatasetType result = 
                    cachingSrv.getDxf( provider, service, ns, ac, detail );
                if ( result != null ) {
                    dataset.value = result;
                } else {
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }
            
            if ( format != null && (format.equalsIgnoreCase( "native" ) || format.equalsIgnoreCase( "both" )) ) {
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service, ns, ac );
                
                if ( natRec != null && natRec.getNativeXml() != null && natRec.getNativeXml().length() > 0 ) {
                    nativerecord.value = natRec.getNativeXml();
                    timestamp.value = 
                        TimeStamp.toXmlDate( natRec.getQueryTime() );
                } else {
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

        } catch ( ProxyFault sf ) {
            
            throw sf; // pass EbiFault

        } catch ( ServiceException se ) { // pass exception
            String message = se.getServiceFault().getMessage();
            log.warn( "EbiCachingImpl: ServiceException: message= " + message);
            
            ProxyFault fault = new ProxyFault(message, se.getServiceFault());
            throw fault; 

        } catch ( Exception e ) {
            log.warn( "EbiCachingImpl: " + e.toString() );
            ServiceFault fault = new ServiceFault();
            fault.setMessage(e.toString());
            fault.setFaultCode(99);
            ProxyFault proxyFault = new ProxyFault(e.toString(), fault);
            throw proxyFault;
        }
    }
}
