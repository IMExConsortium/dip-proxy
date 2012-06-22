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
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.TimeStamp;

import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.proxy.router.Router;
import edu.ucla.mbi.server.WSContext;

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
  
        //*** validation of ac 
        if ( ac == null || ac.equals( "" ) ) {
            log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
        }

        //*** validation of detail
        if ( detail == null || detail.equals("") ) {
            detail = "stub";

        } else if ( detail.equalsIgnoreCase( "short" ) ||
                    detail.equalsIgnoreCase( "stub" ) ) 
        {
            detail = "stub";
        } else if ( detail.equalsIgnoreCase( "base" ) ){
            detail = "base";
        } else {
            detail = "full";
        }

        //*** validation of format
        if( format == null || format.equals( "" ) ) {
            format = "dxf";
        } else if ( !format.equalsIgnoreCase( "native" ) 
                        && !format.equalsIgnoreCase( "dxf" ) 
                        && !format.equalsIgnoreCase( "both" ) ) 
        {
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP ); 
        }

        //validation of ns 
        if ( provider.equals( "NCBI" ) ) {
            if( service.equals( "nlm" ) ) {
                log.info( " forcing nlm as ns. " );
                ns = "nlmid";
            } else if ( service.equals( "taxon" ) ) { 
                log.info( "forcing ncbitaxid as ns. " );
                ns = "ncbitaxid";
            } else if ( service.equals("pubmed") ) { 
                log.info( "forcing pmid as ns. " );
                ns = "pmid";
            } else if ( service.equals( "refseq" ) ) {
                ns = "refseq";
            } else if ( service.equals( "entrezgene" ) ) {
                ns = "entrezgene";
            } else {
                throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP ); 
            }
        } else if ( provider.equals( "EBI" ) ) {
            if( service.equals( "uniprot" ) ) { 
                log.info( "getRecord: forcing uniprot as ns" );
                ns = "uniprot";
            } else if ( service.equals( "picr" ) ) {
                if( detail.equalsIgnoreCase( "stub" ) ) {
                    detail = "base"; // picr cann't support detail with stub
                }
            } else {
                throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP ); 
            }
        } else if ( provider.equals( "DIP" ) ) {
            if( service.equals( "dip" ) ) {
                ns = "dip";
            } else {
                throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP ); 
            }
        } else if ( provider.equals( "MBI" ) ) {
            if( service.equals( "prolinks" ) ) {
                ns = "refseq";
            } else {
                throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP ); 
            }
        } else {
            throw FaultFactory.newInstance( 4 ); //unsupported operation
        }

        log.info( "getRecord: provider=" + provider 
                  + " and service=" + service + " ac=" + ac 
                  + " and detail=" + detail + " format=" + format );
        
        try {
            Router router =
                WSContext.getServerContext( provider ).createRouter() ;

            log.info( "getRecord: router=" + router );
            log.info( "getRecord: router.rsc=" 
                       + router.getRemoteServerContext().getProvider() );

            CachingService cachingSrv =
                new CachingService( provider, router,
                                    WSContext.getServerContext( provider ) );

            if ( format.equalsIgnoreCase( "dxf" ) 
                    || format.equalsIgnoreCase( "both" ) ) 
            {
                log.info( "getRecord: before cachingService getDxf. " );
                DatasetType result =
                    cachingSrv.getDxf( provider, service, ns, ac, detail );

                if ( result != null ) {
                    dataset.value = result ;
                } else {
                    log.info("getRecord: return dataset is null ");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }
            
            if ( format.equalsIgnoreCase( "native" ) 
                    ||  format.equalsIgnoreCase( "both" ) )
            {
                log.info( "getRecord: before cachingService getNative. " );
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

        }catch ( ProxyFault fault ) {
            String message = fault.getFaultInfo().getMessage();
            log.warn( "ProxyPortImpl: ProxyFault: message= " + message);
            throw fault;

        } catch ( Exception e ) {
            log.warn( "ProxyPortImpl: " + e.toString() );
            throw  FaultFactory.newInstance( Fault.UNKNOWN );
        }

        return;
    }

}
