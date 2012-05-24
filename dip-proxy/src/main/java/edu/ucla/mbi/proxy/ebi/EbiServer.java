package edu.ucla.mbi.proxy.ebi;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * EbiServer:
 *    services provided by EBI web services
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URL;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.bind.*;
import javax.xml.namespace.QName;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceException;

// EBI Picr client
//----------------

import uk.ac.ebi.picr.*;
import uk.ac.ebi.picr.accessionmappingservice.*;
import uk.ac.ebi.picr.model.*;

public class EbiServer extends RemoteNativeServer {

    List<String> searchDB;

    String picrEndpoint = null;
    
    private final String 
        nsPicr = "http://www.ebi.ac.uk/picr/AccessionMappingService";

    private final String
        nmPicr = "AccessionMapperService";
    
    /*
     * generate static instance of JAXBContext see
     * https://jaxb.dev.java.net/faq/index.html#threadSafety for details.
     */

    static final JAXBContext acrContext = initAcrContext();
    
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
    
    public void initialize() {
        Log log = LogFactory.getLog( EbiServer.class );
        log.info( "initializing: " );

        searchDB = new ArrayList<String>();

        Map<String, Object> context = getContext();
        
        if ( context != null && context.get( "searchDbList" ) != null ) {
            log.info( " searchDB list: " );
            for ( Iterator ii =
                    ((List) context.get( "searchDbList" )).iterator(); ii
                    .hasNext(); ) {
                String db = (String) ii.next();
                db = db.replaceAll( "\\s+", "" );
                log.info( "  db=" + db );
                searchDB.add( db );
            }
        }
        
        if( context != null ){
            picrEndpoint = (String) getContext().get( "picrEndpoint" );
            if( picrEndpoint != null ){
                picrEndpoint = picrEndpoint.replaceAll( "^\\s+", "" );
                picrEndpoint = picrEndpoint.replaceAll( "\\s+$", "" );
            }
        }
    }
    
    //-------------------------------------------------------------------------
    
    public NativeRecord getNative( String provider, String service, 
                                   String ns, String ac, int timeOut 
                                   ) throws ServiceException {
        
        Log log = LogFactory.getLog( EbiServer.class );

        String retVal = null;

        log.info( "NS=" + ns + " AC=" + ac + " SRV=" + service );

        if ( service.equals( "uniprot" ) ) {

            // call EBI WSDbfetch utility: REST version

            String url = getRestUrl();
            String acTag = getRestAcTag();

            log.info( " restURL=" + url );
            log.info( " restTag=" + acTag );

            url = url.replaceAll( acTag, ac );

            try {
                retVal = NativeURL.query( url, timeOut );

                if ( retVal == null ) {
                    log.warn("EbiServer: getNative:" + 
                             " uniprot service for AC " + ac +
                             ": no record found.");
                    throw Fault.getServiceException( Fault.NO_RECORD );
                }
            } catch ( ServiceException ex ) {
                throw ex;
            } catch ( Exception e ) {
                log.info( "EbiServer:" + 
                          " getUniprot: exception: " + e.toString() );
                if ( e.toString().contains( "No result found" ) ) {
                    log.warn( "EbiServer: getNative:" +
                              " uniprot service for AC " + ac +
                              ": no record found." );
                    throw Fault.getServiceException( Fault.NO_RECORD );
                } else if ( e.toString().contains( "Read timeout" ) ) {
                    log.warn( "EbiServer: getNative:" +
                              " uniprot service for AC " + ac +
                              ": remote server timeout." );
                    throw Fault.getServiceException( Fault.REMOTE_TIMEOUT );
                } else {
                    log.warn("EbiServer: getNative:" +
                             " uniprot service for AC " + ac +
                             ": get exception: " + e.toString() + ". ");
                    throw Fault.getServiceException( Fault.UNKNOWN );
                }
            }
        }

        //----------------------------------------------------------------------

        if ( service.equals( "picr" ) ) {
            try {

                // call EBI PICR utility

                AccessionMapperInterface port = null;
                
                try{
                    AccessionMapperService amSrv = 
                        new AccessionMapperService( new URL( picrEndpoint 
                                                             + "?wsdl" ),
                                                    new QName( nsPicr, nmPicr ) 
                                                    );
                    port = amSrv.getAccessionMapperPort();

                } catch(Exception ex){
                    log.warn( "EbiServer: picr endpoint not set.");
                    throw ex;
                }
                

                List<UPEntry> entries =
                        port.getUPIForAccession( ac, "", searchDB, "", true );

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
                    throw Fault.getServiceException( Fault.NO_RECORD );
                }
            } catch (ServiceException fault){
                throw fault;                
            } catch ( Exception e ) {

                log.info( "EbiNativeThread: getPicrList:" +
                          " exception: " + e.toString() );

                if ( e.toString().contains( "Read timeout" ) ) {
                    throw Fault.getServiceException( Fault.REMOTE_TIMEOUT );
                } else if ( e.toString().contains( "HTTP Status-Code 404: Not Found") ){
                    log.warn( "EbiServer: getNative:" +
                              " picr service for AC " + ac +
                              ": remote server is not available." );
                    throw Fault.getServiceException( Fault.REMOTE_FAULT);
                } else if( e.toString().contains( "Unsupported Content-Type: text/html") ){
                    log.warn( "EbiServer: getNative:" +
                              " picr service for AC " + ac +
                              ": unsupported Content-Type: text/html.");
                    throw Fault.getServiceException( Fault.REMOTE_FAULT);
                } else {
                    log.warn( "EbiServer: getNative:" +
                              " picr service for AC " + ac +                                             
                              ": get exception: " + e.toString() + ". ");
                    throw Fault.getServiceException( Fault.UNKNOWN );
                }
            }
        }
        
        NativeRecord record = new NativeRecord( provider, service, ns, ac);
        record.setNativeXml( retVal );
        return record;
    }
}
