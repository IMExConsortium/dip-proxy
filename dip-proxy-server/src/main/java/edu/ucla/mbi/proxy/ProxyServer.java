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
import edu.ucla.mbi.proxy.context.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.datatype.XMLGregorianCalendar;


public class ProxyServer extends ConfigurableServer {

    private Log log = LogFactory.getLog( ProxyServer.class );

    public ProxyServer () {}

    public ProxyServerRecord getRecord( String provider, String service,
                                        String ns, String ac, String match,
                                        String detail, String format,
                                        String client, Integer depth ) 

        throws ServerFault{

        DatasetType dataset = null;
        String nativeRecord = null;
        XMLGregorianCalendar timestamp = null;
    
        ProxyServerRecord psr = null;

        log.info( "provider=" + provider + ", service=" + service + 
                  ", ns=" + ns + ", ac=" + ac );
                
        ns = this.validateNs ( provider, service, ns, ac, detail, format );

        log.info( "after validateNs: ns=" + ns );
        // get record from the server
        
                   
        CachingService cachingSrv =
            new CachingService( wsContext, provider  );
            
        if ( format.equalsIgnoreCase( "dxf" )
             || format.equalsIgnoreCase( "both" ) ) {

            DxfRecord dxfRec = 
                cachingSrv.getDxfRecord ( provider, service, ns, ac, detail );
            
            if( dxfRec != null && dxfRec.getDxf() != null ) {
                
                dataset = cachingSrv.getDatasetType ( dxfRec );
                
                if( dataset != null ) {
                    timestamp  =
                        TimeStamp.toXmlDate( dxfRec.getQueryTime() );
                    
                } else {
                    throw ServerFaultFactory.newInstance( Fault.MARSHAL );
                }
            }
        }

        if ( format.equalsIgnoreCase( "native" )
             ||  format.equalsIgnoreCase( "both" ) ) {
            
            NativeRecord natRec =
                cachingSrv.getNative( provider, service, ns, ac );
            
            if ( natRec != null && natRec.getNativeXml() != null &&
                 natRec.getNativeXml().length() > 0 ) {
                
                nativeRecord = natRec.getNativeXml();
                log.info( "natRecord =" + nativeRecord.substring(0, 200 ) );
                
                log.info( "natRec queryTime=" + natRec.getQueryTime() );
                timestamp =
                    TimeStamp.toXmlDate( natRec.getQueryTime() );
                
            }
        }
        
        if( dataset != null || nativeRecord != null ) { 

            psr = new ProxyServerRecord( dataset, nativeRecord, timestamp);
        }

        return psr;
    }

    private String validateNs ( String provider, String service,
                                String ns, String ac, String detail, 
                                String format ) 
        throws ServerFault {


        log.debug( "ProxyServer: wsContext=" + wsContext);
        
        //*** validation of provider and service
        if ( provider == null || provider.equals( "" )
                || service == null || service.equals( "" ) ) {
            log.info( "provider or server is missed" );
            throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        provider = provider.toUpperCase();

        if( wsContext.getProvider( provider ) == null) {
            log.info( "This provider(" + provider + ") doesn't exist " +
                      "in the server. " );
            throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        if( !wsContext.getServerContext( provider )
                        .getServiceSet().contains( service ) ) {

            log.info( "This service(" + service + ") doesn't exist " +
                      "in the server. " );
            throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        
        log.info( "provider=" + provider + " and service=" + service + ". " );        

        //*** validation of ac 
        if ( ac == null || ac.equals( "" ) ) {
            log.info( "missing accession" );
            throw ServerFaultFactory.newInstance( Fault.MISSING_ID );
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
            
            throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP ); 
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
            throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        log.info( "getRecord: ns= " + ns + " and ac=" + ac +
                  " and detail=" + detail + " format=" + format + "." );

        return ns;
    } 
}
