package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/main$
 * $Id:: NcbiServer.java 2607 2012-07-31 20:38:53Z wyu                         $
 * Version: $Rev:: 2607                                                        $
 *==============================================================================
 *
 * NCBIServer:
 *    services provided by NCBI web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.*;
import java.io.*;
import java.util.*;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.*;

import javax.servlet.ServletContext;
import org.springframework.web.context.ServletContextAware;

public class NativeRestServer implements NativeServer, ServletContextAware {

    private Log log = LogFactory.getLog( NativeRestServer.class );
    private  Map<String,Object> restServerMap = new HashMap<String, Object>();   
    private JsonContext restServerContext;
    private ServletContext servletContext;
    public static  String restServerJFP;
 
    public Map<String,Object> getRestServerMap() {
        return restServerMap;
    }

    public void setRestServerContext( JsonContext context ) {
        this.restServerContext = context;
    }

    public JsonContext getRestServerContext() {
        return restServerContext;
    }

    public void setServletContext ( ServletContext servletContext ) {
        this.servletContext = servletContext;
    }

    public void initialize() throws ProxyFault {
        log.info( "initialize starting... " );
        configInitialize();
    }

    public String getRestServerJFP () {
        return restServerJFP;
    }

    public void configInitialize() throws ProxyFault {

        log.info( "restServerConfigInitialize starting ... " );

        try {
            restServerJFP = 
                    JsonUnderServletContext.getPathAfterReadJson ( 
                                    restServerContext, servletContext );
        } catch( Exception e ) {
            throw FaultFactory.newInstance ( 27 ); // json configuration
        }

        log.info( "restServerConfigInitialize: restServerJFP=" + restServerJFP );
       
        Map<String, Object> jrs = restServerContext.getJsonConfig(); 
        
        restServerMap = (Map) jrs.get( "restServer" );

        log.info( "restServerConIni ... after get restSerer . " );
    }

    public String getRealUrl ( String provider, String service, String ac ) 
                                                            throws ProxyFault 
    {

        if( restServerMap.get(provider) == null ) {
            log.warn( "getRealUrl: provider=" + provider + " does not exist. " );
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }
        
        if( ( (Map<String, Map>)restServerMap.get(provider) ).get(service)
                == null ) 
        { 
            log.warn( "getRealUrl: service=" + service + " does not exist. " );
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        String restAcTag =
           (String) ( (ArrayList) ( (Map<String, Map>) (
                            (Map<String, Map>)restServerMap.get(provider) )
                                             .get(service) ).get("restAcTag") ).get(0);

        String restUrl =
            (String) ( (ArrayList) ( (Map<String, Map>) (
                            (Map<String, Map>)restServerMap.get(provider) )
                                            .get(service) ).get( "restUrl" ) ).get(0);

        if( restAcTag == null || restUrl == null ) {
            log.warn( "getRealUrl: restAcTag or restUrl is not configured. " );
            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        } 

        restAcTag = restAcTag.replaceAll( "^\\s+", "" );
        restAcTag = restAcTag.replaceAll( "\\s+$", "" );

        restUrl = restUrl.replaceAll( "^\\s+", "" );
        restUrl = restUrl.replaceAll( "\\s+$", "" );
       
        return restUrl.replaceAll( restAcTag, ac );
    }

    public NativeRecord getNative( String provider, String service, 
                                   String ns, String ac, int timeout 
                                   ) throws ProxyFault 
    {
        String retVal = null;
        log.info( "getNative: PROVIDER=" + provider + " and SERVICE=" + 
                  service + " and NS=" + ns + " AC=" + ac );
        
        String real_restUrl = getRealUrl( provider, service, ac );

        log.info( "getNative: real_restUrl=" + real_restUrl );

        try {
            retVal = query( real_restUrl, timeout );
            
        } catch( ProxyFault fault ) {
            throw fault;
        }

        if( retVal.endsWith( "</cause></error></ResultSet>" ) ) {
            //*** this error for intermine server
            log.warn( "getNative: return error=" + retVal );
            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        if( retVal.endsWith( "<ResultSet ></ResultSet>" ) ) {
            //**** this fault for intermine server
            log.warn( "getNative: return an empty set. " );
            throw FaultFactory.newInstance( Fault.NO_RECORD );
        }

        if( retVal.endsWith( "<TaxaSet></TaxaSet>" ) ) {
             //**** this fault for NCBI taxon server
            log.warn( "getNative: return an empty set. " );
            throw FaultFactory.newInstance( Fault.NO_RECORD );
        }

        if( retVal.contains("<INSDSet><Error>")
                    || retVal.contains( "<TSeqSet/>" ) ) {

            //*** this fault for NCBI refseq
            log.warn( "getNative: refseq get wrong retVal for ac " + ac + "." );
            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        //** this fault for MBI prolinks
        if( retVal.contains( "<faultCode>97</faultCode>" ) ) {
            log.info( "getNative: return faultCode 97. " );
            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
        } else if ( retVal.contains( "<faultCode>5</faultCode>" ) ) {
            log.info( "getNative: return faultCode 5. " );
            throw FaultFactory.newInstance( Fault.NO_RECORD );
        } else if ( retVal.contains( "</faultCode>") ) {
            log.warn( "getNative: return faultCode=" + retVal );
            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        NativeRecord record = new NativeRecord( provider, service, ns, ac );
        record.setNativeXml( retVal );
        return record;

    }

    private String query( String url, int timeout ) throws ProxyFault {
        String retVal = "";

        try {
            java.net.URL xmlURL = new URL( url );
            java.net.HttpURLConnection conn
                    = (HttpURLConnection) xmlURL.openConnection();
            
            if( conn.getResponseCode() == 404 ) {
                //*** this fault for EBI uniprot
                log.warn( "query: connection get 404 code ( Not Found). " );
                throw FaultFactory.newInstance( Fault.NO_RECORD );
            }
            
            if( conn.getResponseCode() != 200 ) {
                log.warn ( "query: connectin get response message: "
                           + conn.getResponseMessage() );

                if( conn.getResponseMessage().equals( "Bad Request" ) ) {
                    //*** this fault for NCBI refseq
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                } else {
                    throw new IOException( conn.getResponseMessage() );
                }
            }

            // setting timeout ensure the client does not deadlock indefinitely
            // -----------------------------------------------------------------
            conn.setConnectTimeout( timeout ); // uTimeout is int as milliseconds
            conn.setReadTimeout( timeout );


            BufferedReader in =
                new BufferedReader( new InputStreamReader( conn.getInputStream() ) );

            if ( in != null ){
                String inputLine = new String();
                StringBuffer sb = new StringBuffer();
                while( ( inputLine = in.readLine() ) != null ) {
                    sb.append(inputLine);
                }
                retVal = sb.toString();
            }

            in.close();
            conn.disconnect();

        } catch ( ProxyFault fault ) {
            throw fault;
        } catch ( Exception e ) {
            log.info( "query: exception: " + e.toString());
            if( e.toString().contains( "TimeoutException" )
                    || e.toString().contains( "Read timeout" ) )
            {
                throw FaultFactory.newInstance( Fault.REMOTE_TIMEOUT );  // timeout
            } else if ( e.toString().contains( "No result found" ) ) {
                throw FaultFactory.newInstance( Fault.NO_RECORD );
            } else {
                //*** including http status 503 Service Temporarily Unavailable
                //***   and wrong url address
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );  // unknown remote
            }
        }

        if( retVal == null ) {
            log.info( "query:  return null. " );
            throw FaultFactory.newInstance( Fault.NO_RECORD ); // no hits
        } else {
            return retVal;
        }
    }

}
