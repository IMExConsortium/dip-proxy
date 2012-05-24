package edu.ucla.mbi.proxy.dip;

/*=========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * DipCachingImpl - Dip Database access implemented 
 * through SOAP service
 *
 *
 *======================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;

import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceException;
import edu.ucla.mbi.services.TimeStamp;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import edu.ucla.mbi.proxy.router.Router;

import javax.xml.datatype.XMLGregorianCalendar;

@WebService(endpointInterface = "edu.ucla.mbi.proxy.DipProxyPort")
public class DipCachingImpl implements DipProxyPort {

    /*
     * Fetch record from Dip
     */

    public void getDipRecord( String ns, String ac, String match,
                              String detail, String format, 
                              String client, Integer depth,
                              Holder<XMLGregorianCalendar> timestamp,
                              Holder<DatasetType> dataset, 
                              Holder<String> nativerecord )
        throws ProxyFault {
        
        String provider = "DIP";
        String service = "dip";

        Log log = LogFactory.getLog( DipCachingImpl.class );
        log.info( "getPubmedArticle " + " NS=" + ns + " AC=" + ac + " DT="
                + detail );

        if ( !ns.equalsIgnoreCase( "dip" ) ) {
            log.info( " forcing pubmed as ns" );
            ns = "dip";
        }

        if ( ac == null || ac.equals( "" ) ) {
            log.info( "DipCaching: missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );            
        }

        if ( detail == null ) {
            detail = "stub";
        } else if ( detail.equalsIgnoreCase( "short" )
                || detail.equalsIgnoreCase( "stub" ) ) {
            detail = "stub";
        } else {
            detail = "full";
        }
        try {
            Router router = WSContext.getServerContext( provider )
                    .createRouter();
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
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

            if ( format != null
                 && (format.equalsIgnoreCase( "native" ) || format
                     .equalsIgnoreCase( "both" )) ) {
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service,
                                          ns, ac );
                
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
            log.info( "ServiceFault: " + se.toString() );
            throw  FaultFactory.newInstance( se.getServiceFault() );
        } catch ( Exception e ) {
            log.info( "DipCachingImpl: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
    }
}
