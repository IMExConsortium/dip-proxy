package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProxyServer - dip-proxy services implemented 
 *                                  
 *=========================================================================== */

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.TimeStamp;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.proxy.router.Router;
import edu.ucla.mbi.server.*;

import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;

public class ProxyServer extends ConfigurableServer {

    private Log log = LogFactory.getLog( ProxyServer.class );

    public ProxyServerRecord getRecord( String provider, String service,
                                        String ns, String ac, String match,
                                        String detail, String format,
                                        String client, Integer depth ) 

        throws ProxyServerFault{

        // get record from the server

        ProxyServerRecord rec = 
            new ProxyServerRecord( dataset, nativerecord, timestamp)
        
        return rec;
        
    }

    private String validateCheck ( String provider, String service,
                                   String ns, String ac, String detail, 
                                   String format
                                   )
        throws ProxyServerFault{

        return ns;
        
        /*
        //*** validation of provider and service
        if ( provider == null || provider.equals( "" )
                || service == null || service.equals( "" ) ) {
            log.info( "provider or server is missed" );
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        provider = provider.toUpperCase();

        if( wsContext.getProvider( provider ) == null) {
            log.info( "This provider(" + provider + ") doesn't exist " +
                      "in the server. " );
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        if( !wsContext.getServerContext( provider )
                        .getServiceSet().contains( service ) ) {

            log.info( "This service(" + service + ") doesn't exist " +
                      "in the server. " );
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        
        log.info( "provider=" + provider + " and service=" + service + ". " );        

        //*** validation of ac 
        if ( ac == null || ac.equals( "" ) ) {
            log.info( "missing accession" );
            throw FaultFactory.newInstance( Fault.MISSING_ID );
        }

        //*** validation of detail
        if ( detail == null || detail.equals("") ) {
            detail = "stub";

        } else if ( detail.equalsIgnoreCase( "short" ) 
                    || detail.equalsIgnoreCase( "stub" ) ) {

            detail = "stub";

        } else if ( detail.equalsIgnoreCase( "base" ) ) {
            detail = "base";
        } else {
            detail = "full";
        }

        //*** validation of format
        if( format == null || format.equals( "" ) ) {

            format = "dxf";

        } else if ( !format.equalsIgnoreCase( "native" ) 
                    && !format.equalsIgnoreCase( "dxf" ) 
                    && !format.equalsIgnoreCase( "both" ) ) {

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
            }
        } else if ( provider.equals( "EBI" ) ) {
            if( service.equals( "uniprot" ) ) { 
                log.info( "getRecord: forcing uniprot as ns" );
                ns = "uniprot";
            } else if ( service.equals( "picr" ) ) {
                if( detail.equalsIgnoreCase( "stub" ) ) {
                    detail = "base"; // picr cann't support detail with stub
                }
            }
        } else if ( provider.equals( "DIP" ) ) {
            log.info( "getRecord: provider is DIP. and service=" + service );
            if( service.equals( "dip" ) || service.equals( "diplegacy" ) ) {
                log.info( "getRecord: forcing dip as ns. " );
                ns = "dip";
            }
        } else if ( provider.equals( "MBI" ) ) {
            if( service.equals( "prolinks" ) ) {
                ns = "refseq";
            }
        } else if ( provider.equals( "SGD" ) ) {
            if( service.equals( "yeastmine" ) ) {
                ns= "sgd";
            } 
        }

        if( ns == null || ns.equals( "" ) ) {
            log.info( " ns is missed. " );
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        log.info( "getRecord: ns= " + ns + " and ac=" + ac +
                  " and detail=" + detail + " format=" + format + "." );

        return ns;
        */
    } 
}
