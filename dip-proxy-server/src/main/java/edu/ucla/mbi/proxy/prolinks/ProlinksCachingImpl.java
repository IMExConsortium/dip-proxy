package edu.ucla.mbi.proxy.prolinks;

/* =============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *                                                                        
 * ProlinksCachingImpl - Prolinks Database access implemented             
 * through ....                                                           
 *                                                                        
 *  NOTE: Modify gen-src/axis2/prolinks/resources/services.xml to use     
 *  this instead of default ProlinksPublicSkeleton.                       
 *                                                                        
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.server.*;

import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.TimeStamp;

import javax.xml.datatype.XMLGregorianCalendar;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import edu.ucla.mbi.proxy.router.Router;

@WebService(endpointInterface = "edu.ucla.mbi.proxy.ProlinksProxyPort")
public class ProlinksCachingImpl extends ConfigurablePortImpl 
    implements ProlinksProxyPort {

    public void getProlinks( String ns, String ac, String match, 
                             String detail, String format, 
                             String client, Integer depth,
                             Holder<XMLGregorianCalendar> timestamp,
                             Holder<DatasetType> dataset, 
                             Holder<String> nativerecord )
        throws ProxyFault {
        
        String provider = "MBI";
        String service = "prolinks";

        Log log = LogFactory.getLog( ProlinksCachingImpl.class );
        log.info( "getProlinks " + 
                  " NS=" + ns + " AC=" + ac + " DT=" + detail );
        
        if ( !ns.equalsIgnoreCase( "refseq" ) ) {
            log.info( " forcing refseq as ns" );
            ns = "refseq";
        }

        if ( ac == null || ac.equals( "" ) ) {
            log.info( "missing accession" );
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

        RemoteServerContext rsc = context.getServerContext( provider );

        Router router = rsc.getRouter();

        if( rsc == null || router == null ) {
            log.warn( "rsc or router is null for the provider(" + provider +
                      "). " );
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        try {

            CachingService cachingSrv = 
                new CachingService( provider, router, rsc );
            
            if ( format == null || format.equals( "" )
                 || format.equalsIgnoreCase( "dxf" )
                 || format.equalsIgnoreCase( "both" ) ) {
                
                DatasetType result
                    = cachingSrv.getDatasetType( provider, service,
                                                 ns, ac, detail );
                if ( result != null ) {
                    dataset.value = result;
                } else {
                    log.info( "return dataset is null" );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

            if ( format != null
                 && ( format.equalsIgnoreCase( "native" ) 
                      || format.equalsIgnoreCase( "both" ) ) ) {
                
                NativeRecord natRec = 
                    cachingSrv.getNative( provider, service, ns, ac );

                if ( natRec != null ) {
                    
                    nativerecord.value = natRec.getNativeXml();
                    timestamp.value = 
                        TimeStamp.toXmlDate( natRec.getCreateTime() );

                } else {
                    log.info( "return dataset is null " );
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }
            }

        } catch ( ProxyFault fault ) {
            log.info( "getProlinks fault: " + fault.getFaultInfo().getMessage() );
            throw fault;
        } catch ( Exception e ) {
            log.info( "getProlinks fault: " + e.toString() );
            throw FaultFactory.newInstance( Fault.UNKNOWN );
        }
    }
}
