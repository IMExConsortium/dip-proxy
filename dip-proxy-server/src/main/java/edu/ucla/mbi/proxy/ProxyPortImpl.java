package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProxyPortImpl - dip-proxy services implemented 
 *                                  
 *=========================================================================== */

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceException;
import edu.ucla.mbi.services.ServiceFault;
import edu.ucla.mbi.services.TimeStamp;

import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.proxy.router.Router;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;

@WebService(endpointInterface="edu.ucla.mbi.proxy.ProxyPort")

public class ProxyPortImpl implements ProxyPort {

    private Log log = LogFactory.getLog( ProxyPortImpl.class );

    public void getRecord( String provider, String service,
                           String ns, String ac, String match,
                           String detail, String format,
                           String client, Integer depth,
                           Holder<XMLGregorianCalendar> timestamp,
                           Holder<DatasetType> dataset,
                           Holder<String> nativerecord
                           ) throws ProxyFault {
   
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

        //validation of ns undone here ??????????
        if ( provider.equals( "NCBI" ) ) {
            if( service.equals( "nlm" ) && !ns.equalsIgnoreCase( "nlmid" ) ) {
                log.info( " forcing nlm as ns" );
                ns = "nlmid";
            } else if ( service.equals( "taxon" ) 
                            && !ns.equalsIgnoreCase( "ncbitaxid" ) ) 
            {
                log.info( "forcing ncbitaxid as ns" );
                ns = "ncbitaxid";
            }  
        } else if ( provider.equals( "EBI" ) ) {
            if( service.equals( "uniprot" ) 
                    && !ns.equalsIgnoreCase( "uniprot" ) ) 
            {
                log.info( "EbiCaching: forcing uniprot as ns" );
                ns = "uniprot";
            }
        } else if ( provider.equals( "DIP" ) ) {

        } else {
            throw FaultFactory.newInstance( 8 ); // invalid query type
        }

        
        try {
            Router router =
                WSContext.getServerContext( provider ).createRouter() ;

            CachingService cachingSrv =
                new CachingService( provider, router,
                                    WSContext.getServerContext( provider ) );

            if ( format == null || format.equals( "" ) ||
                 format.equalsIgnoreCase( "dxf" ) ||
                 format.equalsIgnoreCase( "both" ) ) {

                DatasetType result =
                    cachingSrv.getDxf( provider, service, ns, ac, detail );
                if ( result != null ) {
                    dataset.value = result ;
                } else {
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }
            
            if ( format != null &&
                 ( format.equalsIgnoreCase( "native" ) ||
                   format.equalsIgnoreCase( "both" ) )
                 ) {

                NativeRecord natRec =
                    cachingSrv.getNative( provider, service, ns, ac );

                if ( natRec != null && natRec.getNativeXml() != null &&
                     natRec.getNativeXml().length() > 0 ) {
                    nativerecord.value = natRec.getNativeXml();
                    timestamp.value =
                        TimeStamp.toXmlDate( natRec.getQueryTime() );
                } else {
                    log.info("return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

        }catch ( ServiceException se ) {
            String message = se.getServiceFault().getMessage();
            log.warn( "ProxyPortImpl: ServiceException: message= " + message);

            ProxyFault fault = new ProxyFault(message, se.getServiceFault());
            throw fault;

        } catch ( Exception e ) {
            log.warn( "ProxyPortImpl: " + e.toString() );
            ServiceFault fault = new ServiceFault();
            fault.setMessage(e.toString());
            fault.setFaultCode(99);
            ProxyFault proxyFault = new ProxyFault(e.toString(), fault);
            throw proxyFault;
        }

        return;
    }

}
