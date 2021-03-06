package edu.ucla.mbi.proxy;
 
/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/main$
 * $Id:: RestServer.java 3316 2013-07-10 17:26:06Z wyu                         $
 * Version: $Rev:: 3316                                                        $
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

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.context.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class RestServer implements ContextListener {

    private Log log = LogFactory.getLog( RestServer.class );
    
    private  Map<String,Object> restServerMap = new HashMap<String, Object>();   
    private JsonContext restServerContext;
    private String contextTop;

    private Map<String, Object> context = null;
    
    //--------------------------------------------------------------------------

    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }

    //--------------------------------------------------------------------------

    public Map<String,Object> getRestServerMap() {
        return restServerMap;
    }

    //*** setter
    public void setRestServerContext( JsonContext context ) {
        this.restServerContext = context;
    }

    public void setContextTop( String top ) {
        this.contextTop = top;
    }
    
    //*** getter
    public JsonContext getRestServerContext() {
        return restServerContext;
    }

    public String getContextTop() {
        return contextTop;
    }

    int throttle = 0;
    
    public void setThrottle(int throttle){
        this.throttle = throttle;
    }

    public int  getThrottle(){
        return this.throttle;
    }
    
    public void initialize() throws ServerFault {

        log.info( "RestServer initialize starting... " );
        
        if(  context == null ) {
            log.warn( "RestServer: initializing failed " +
                      "because context is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        restServerContext = (JsonContext)context.get( "restServerContext" );

        if( restServerContext == null ) {
            log.warn( "RestServer: initializing failed " +
                      "because restServerContext is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        contextTop = (String) context.get( "contextTop" );
        if( contextTop == null ) {
            log.warn( "RestServer: initializing failed " +
                      "because contextTop is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        FileResource fr = (FileResource) restServerContext
                                .getConfig().get("json-source");

        if ( fr == null ) return;

        try {
            restServerContext.readJsonConfigDef( fr.getInputStream() );
        } catch ( Exception e ) {
            log.info( "initialize exception: " + e.toString() );
            throw ServerFaultFactory.newInstance ( Fault.JSON_CONFIGURATION ); 
        }

        Map<String, Object> jrs = restServerContext.getJsonConfig(); 
        
        restServerMap = (Map) jrs.get( contextTop );

        //*** add context listener
        //restServerContext.addContextUpdateListener( this );

    }
    
    public void contextUpdate ( JsonContext context ) {
   
        log.info( "contextUpdate called. " );
         
        Map<String, Object> jrs = context.getJsonConfig();

        restServerMap = (Map) jrs.get( contextTop );
    }

    public String getNativeString( String provider, String service,
                                   String ns, String ac, int timeout
                                   ) throws ServerFault {
        
        String retVal = null;
        
        String real_restUrl = this.getRealUrl( provider, service, ns, ac );
        
        log.info( "getNativeString: real_restUrl=" + real_restUrl );
        
        try {
            retVal = this.query( real_restUrl, timeout );

            //log.info( "NativeString:\n" + retVal+"\n");
        } catch( ServerFault fault ) {
            throw fault;
        }
        
        return retVal;
    }
    
    public Document getNativeDom( String provider, String service,
                                  String ns, String ac  
                                  ) throws ServerFault {
        
        String url_string =
            this.getRealUrl( provider, service, ns, ac );
        
        log.debug( "RestServer.getNativeDom: called" );
        log.debug( "getNativeDom: url_string=" + url_string );
        
        DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();        

        InputStream stream = null;
        HttpURLConnection url_connection = null; 
        try {
            
            DocumentBuilder builder = fct.newDocumentBuilder();
            
            URL url_search = new URL( url_string );
            log.info( "RestServer.getNativeDom:  url_search OK" );

            url_connection = (HttpURLConnection) url_search.openConnection();
            
            log.debug( "RestServer.getNativeDom:  connection OK" );
            log.debug( "RestServer.getNativeDom:  connecion timeout: " + url_connection.getConnectTimeout() );
            log.debug( "RestServer.getNativeDom:  read timeout: " + url_connection.getReadTimeout() );

            //url_connection.connect();
            log.info( "RestServer.getNativeDom:  connected..." );
            
            stream = url_connection.getInputStream();
            
            log.debug( "RestServer.getNativeDom:  stream OK" );
            
            InputSource xml_search =
                new InputSource( url_search.openStream() );
            
            Document docSearch = builder.parse( xml_search );

            log.debug( " NativeDom:" +  getStringFromDocument(docSearch) );

            stream.close();
            Thread.sleep( throttle );
            return docSearch;

        } catch ( Exception ex ) {
            if( stream != null ){

                log.info( "RestServer.getNativeDom closing stream" );
                
                try{
                    stream.close();
                    //url_connection.disconnect();
                } catch( IOException iox ){
                    log.info( "RestServer.getNativeDom fault(1)" );
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                }
            }
            
            log.debug( "RestServer.getNativeDom fault(2)" );
            log.debug( "RestServer.getNativeDom: " +  ex.getClass().getName());
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );            
        } 
    }

    private String getRealUrl( String provider, String service, 
                               String ns, String ac ) throws ServerFault {

        if( restServerMap.get(provider) == null ) {
            log.warn( "getRealUrl: provider=" + provider + " does not exist." );
            throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }
        
        if( ( (Map<String, Map>)restServerMap.get(provider) ).get(service) 
               == null ) { 

            log.warn( "getRealUrl: service=" + service + " does not exist." );
            throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }

        String restAcTag =
            ( (Map<String, String>) (
                            (Map<String, Map>)restServerMap.get(provider) )
                                             .get(service) ).get( "restAcTag" );

        String restUrl =
            ( (Map<String, String>) (
                            (Map<String, Map>)restServerMap.get(provider) )
                                            .get(service) ).get( "restUrl" );

        if( restAcTag == null || restUrl == null ) {
            log.warn( "getRealUrl: restAcTag or restUrl is not configured. " );
            throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        } 

        restAcTag = restAcTag.replaceAll( "^\\s+", "" );
        restAcTag = restAcTag.replaceAll( "\\s+$", "" );

        restUrl = restUrl.replaceAll( "^\\s+", "" );
        restUrl = restUrl.replaceAll( "\\s+$", "" );
       
        return restUrl.replaceAll( restAcTag, ac );
    }
        
    private String query( String url, int timeout ) throws ServerFault {
        String retVal = "";

        try {
            java.net.URL xmlURL = new URL( url );
            java.net.HttpURLConnection conn
                    = (HttpURLConnection) xmlURL.openConnection();

            conn.setRequestProperty("Accept-Charset", "UTF-8");
            
            if( conn.getResponseCode() == 404 ) {
                //*** this fault for EBI uniprot
                log.warn( "query: connection get 404 code ( Not Found). " );
                throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
            }
            
            if( conn.getResponseCode() != 200 ) {
                log.warn ( "query: connectin get response message: "
                           + conn.getResponseMessage() );

                if( conn.getResponseMessage().equals( "Bad Request" ) ) {
                    //*** this fault for NCBI refseq
                    throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                } else {
                    throw new IOException( conn.getResponseMessage() );
                }
            }

            log.debug( "HttpURLConnection: encoding=" + conn.getContentEncoding() );

            // setting timeout ensure the client does not deadlock indefinitely
            // -----------------------------------------------------------------
            conn.setConnectTimeout( timeout ); // uTimeout is int as milliseconds
            conn.setReadTimeout( timeout );

            BufferedReader in =
                new BufferedReader( new InputStreamReader( conn.getInputStream(), "UTF-8" ) );

            if ( in != null ){
                String inputLine = new String();
                StringBuffer sb = new StringBuffer();
                while( ( inputLine = in.readLine() ) != null ) {
                    sb.append(inputLine);
                }
                retVal = sb.toString();
            }

            log.debug( "HttpURLConnection: result=\n" + retVal );

            in.close();
            conn.disconnect();

        } catch ( ServerFault fault ) {
            throw fault;
        } catch ( Exception e ) {
            log.info( "query: exception: " + e.toString());
            if( e.toString().contains( "TimeoutException" )
                    || e.toString().contains( "Read timeout" ) )
            {
                throw ServerFaultFactory.newInstance( Fault.REMOTE_TIMEOUT );  
            } else if ( e.toString().contains( "No result found" ) ) {
                throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
            } else {
                //*** including http status 503 Service Temporarily Unavailable
                //***   and wrong url address
                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );  
            }
        }

        if( retVal == null ) {
            log.info( "query:  return null. " );
            throw ServerFaultFactory.newInstance( Fault.NO_RECORD ); 
        } else {
            return retVal;
        }
    }


    // Document to String conversion

    public String getStringFromDocument( Document doc ){
        try{
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        } catch( TransformerException ex ) {
            ex.printStackTrace();
            return null;
        }
    } 
    
}
