package edu.ucla.mbi.proxy.ebi;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * EbiServer:
 *    services provided by EBI web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URL;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.bind.*;
import javax.xml.ws.BindingProvider;
import com.sun.xml.ws.developer.JAXWSProperties;

import javax.xml.namespace.QName;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;

import uk.ac.ebi.picr.*;
import uk.ac.ebi.picr.accessionmappingservice.*;
import uk.ac.ebi.picr.model.*;

public class EbiServer implements NativeServer {

    private List<String> searchDB;

    private String picrEndpoint = null;
    private AccessionMapperInterface picrPort;    
    
    private final String 
        nsPicr = "http://www.ebi.ac.uk/picr/AccessionMappingService";

    private final String
        nmPicr = "AccessionMapperService";

    /*
     * generate static instance of JAXBContext see
     * https://jaxb.dev.java.net/faq/index.html#threadSafety for details.
     */

    static final JAXBContext acrContext = initAcrContext();

    private NativeServer nativeRestServer = null;
    private Map<String, Object> context = null; 
     
    //--------------------------------------------------------------------------

    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }

    //--------------------------------------------------------------------------
   
    private static JAXBContext initAcrContext() {

        Log log = LogFactory.getLog( EbiServer.class );
        try {
            log.info( " JAXBContext.initAcrContext() called" );
            JAXBContext jbx =
                    JAXBContext.newInstance( "uk.ac.ebi.picr", EbiServer.class
                            .getClassLoader() );
            return jbx;
        } catch ( JAXBException jbe ) {
            log.info( "JAXBContext.initAcrContext(): " + jbe.toString() );
        }
        return null;
    }

    public static JAXBContext getAcrContext() {
        return acrContext;
    }

    //--------------------------------------------------------------------------
    
    public void initialize() throws ServerFault {
        Log log = LogFactory.getLog( EbiServer.class );
        log.info( "initializing: " );

        searchDB = new ArrayList<String>();

        if(  context == null ) {
            log.warn( "EbiServer: initializing failed " + 
                      "because context is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
 
        nativeRestServer = (NativeServer) context.get( "nativeRestServer" );

        if( nativeRestServer == null ) {
            log.warn( "EbiServer: initializing failed " +
                      "because nativeRestServer is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        picrEndpoint = (String) context.get( "picrEndpoint" );
            
        if( picrEndpoint != null && picrEndpoint.length() > 0 ) {
            picrEndpoint = picrEndpoint.replaceAll( "^\\s+", "" );
            picrEndpoint = picrEndpoint.replaceAll( "\\s+$", "" );
        } else {
            log.warn( "EbiServer: PICR service initializing failed " +
                      "because of the picrEndpoint is not set. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        if( context.get( "searchDbList" ) != null ) {
            log.info( " searchDB list: " );
            for ( Iterator ii = 
                    ((List) context.get( "searchDbList" )).iterator(); 
                  ii.hasNext(); ) {

                String db = (String) ii.next();
                db = db.replaceAll( "\\s+", "" );
                log.info( "  db=" + db );
                searchDB.add( db );
            }            
        } else {
            log.warn( "EbiServer: initializing failed " +
                      "because searchDbList is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
        
        //*** call EBI PICR utility
        try{
            AccessionMapperService amSrv =
                new AccessionMapperService( new URL( picrEndpoint + "?wsdl" ),
                                            new QName( nsPicr, nmPicr ) );

            picrPort = amSrv.getAccessionMapperPort();

            if ( picrPort != null ) {
                ((BindingProvider) picrPort).getRequestContext()
                            .put( BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                  picrEndpoint );
            }
        } catch ( Exception ex ) {
            log.warn( "EbiServer: PICR service initializing failed: "
                      + "reason=" + ex.toString() + "." );
            throw ServerFaultFactory.newInstance( Fault.UNKNOWN ); // temporary hiding
        }
    }

    //-------------------------------------------------------------------------
    
    public NativeRecord getNativeRecord( String provider, String service, 
                                         String ns, String ac, int timeout  
                                         ) throws ServerFault { 
    
        Log log = LogFactory.getLog( EbiServer.class );
        log.info( "NS=" + ns + " AC=" + ac + " SRV=" + service );

        if ( !service.equals( "picr" ) ) {
            return nativeRestServer.getNativeRecord( provider, service, 
                                                     ns, ac, timeout );
        } else {

            NativeRecord record = null;
            String retVal = null;
            
            if( picrPort == null ) {
                log.warn( "getNative: picrPort initailizing fault." );
                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
            } else {
                 ((BindingProvider) picrPort).getRequestContext()
                            .put( JAXWSProperties.CONNECT_TIMEOUT, timeout );
            }
            
            try {
                //*** comment: active_only set false
                //***  that will return all active and inactive records
                List<UPEntry> entries =
                //picrPort.getUPIForAccession( ac, "", searchDB, "", true );
                    picrPort.getUPIForAccession( ac, "", searchDB, "", false );
                
                log.info( "EbiServer: got entries: " + entries );
                
                if ( entries != null && entries.size() > 0 ) {

                    log.info( "EbiServer: got entries: #" + entries.size() );
                    uk.ac.ebi.picr.ObjectFactory of =
                            new uk.ac.ebi.picr.ObjectFactory();

                    GetUPIForAccessionResponse response =
                            of.createGetUPIForAccessionResponse();

                    response.getGetUPIForAccessionReturn().addAll( entries );

                    log.info( "EbiServer: got response" );

                    // Marshal object to XML string

                    JAXBContext jc = getAcrContext();
                    Marshaller marshaller = jc.createMarshaller();

                    java.io.StringWriter sw = new StringWriter();
                    marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );
                    marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
                            new Boolean( true ) );

                    log.info( "EbiServer: got marshaller" );

                    marshaller.marshal( response, sw );

                    log.info( "EbiServer: marshalled..." );
                    log.info( "EbiServer: getPicrList: marshal: content \n" +
                              sw.toString().substring( 1, 60 ) );

                    String temp = sw.toString().replaceAll( "ns2:", "" );
                    retVal = 
                        temp.replaceAll( "xmlns(:ns2){0,1}=[\".:-=0-9a-zA-Z/]*", 
                                         "" );
                } else {
                    log.warn("EbiServer: getNative:" +
                             " picr service for AC " + ac +
                             ": no record found." );
                    throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                }
            } catch ( ServerFault fault ) {
                throw fault;                
            } catch ( Exception e ) {

                log.info( "EbiNativeThread: getPicrList:" +
                          " exception: " + e.toString() );

                if ( e.toString().contains( "Read timeout" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_TIMEOUT );
                } else if ( e.toString().contains( "HTTP Status-Code 404: Not Found") ){
                    log.warn( "EbiServer: getNative:" +
                              " picr service for AC " + ac +
                              ": remote server is not available." );
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT);
                } else if( e.toString().contains( "Unsupported Content-Type: text/html") ){
                    log.warn( "EbiServer: getNative:" +
                              " picr service for AC " + ac +
                              ": unsupported Content-Type: text/html.");
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT);
                } else {
                    log.warn( "EbiServer: getNative:" +
                              " picr service for AC " + ac +                                             
                              ": get exception: " + e.toString() + ". ");
                    throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                }
            }

            record = new NativeRecord( provider, service, ns, ac);
            record.setNativeXml( retVal );

            return record;

        } 
    }
}
