package edu.ucla.mbi.proxy.ncbi;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/main$
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NCBIServer:
 *    services provided by NCBI web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import java.io.StringBufferInputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.net.URL;
import java.util.Map;

public class NcbiServer implements NativeServer {
    
    private Log log = LogFactory.getLog( NcbiServer.class );
     
    private Map<String,Object> context = null;

    private NativeRestServer nativeRestServer = null;
    private int threadRunMinutes = 10 ;
    private WSContext wsContext = null;

    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }

    public void initialize() throws ServerFault {
        if( context == null ) {
            log.warn( "NcbiServer: initializing failed " +
                      "because context is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        nativeRestServer = (NativeRestServer) context.get( "nativeRestServer" );
        threadRunMinutes = 
            Integer.parseInt( (String)context.get( "threadRunMinutes" ) );        
        wsContext = (WSContext) context.get( "wsContext" );

        if( nativeRestServer == null && wsContext == null ) {
            log.warn( "NcbiServer: initializing failed " +
                      "because nativeRestServer is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
    }

    public NativeRecord getNative( String provider, String service, String ns,
                                   String ac, int timeout 
                                   ) throws ServerFault {

        log.info( "NcbiServer: NS=" + ns + " AC=" + ac + " OP=" + service );

        //----------------------------------------------------------------------
        if ( !service.equals( "nlm" ) ) {
            return nativeRestServer.getNative( provider, service, 
                                               ns, ac, timeout );
        } else { 
            boolean isRetry = false;
            String retryOn = (String)context.get( "isRetry" );

            if( retryOn != null
                && ( retryOn.equals( "true" )
                     || retryOn.equalsIgnoreCase( "on" )
                     || retryOn.equalsIgnoreCase( "yes" ) ) ) {

                isRetry = true;
            }

            NativeRecord record = null;
            String ncbi_nlmid = "";
        
            try {
                ncbi_nlmid = ((NcbiGetJournal)context.get("ncbiGetJournal"))
                    .esearch( ns, ac, threadRunMinutes, isRetry );

            } catch ( RuntimeException e ) { 
                if( e.getMessage().equals( "NO_RECORD" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                } else {
                    throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                }
            }

            if( ncbi_nlmid.equals( "" ) ) {
                throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
            }

            try {
                record = ((NcbiGetJournal)context.get("ncbiGetJournal"))
                    .efetch( ns, ncbi_nlmid, isRetry );
            } catch ( RuntimeException e ) {
                if( e.getMessage().equals( "NO_RECORD" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                } else {
                    throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                }
            }
            
            return record;
        }
    }       
        /*     
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xPath = xpf.newXPath();
            DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();
       

            //------------------------------------------------------------------
            // esearch ncbi internal id of the nlmid
            //--------------------------------------
            
            try {
                DocumentBuilder builder = fct.newDocumentBuilder();

                // get native rest servet from context

                String url_esearch_string = 
                    nativeRestServer.getRealUrl( provider, "nlmesearch", ac );

                log.info( "getNative: url_esearch_string=" + url_esearch_string );

                URL url_esearch = new URL( url_esearch_string );
                
                InputSource xml_esearch = new InputSource( 
                                                url_esearch.openStream() );

                Document docEsearch = builder.parse( xml_esearch );
                Element rootElementEsearch = docEsearch.getDocumentElement();
              
                // get retry from context 

                if( rootElementEsearch.getChildNodes().getLength() ==  0 ) {

                    log.info( "getNative: nlm esearch get empty return." );

                    if( isRetry ) {
                        try {    
                            NcbiReFetchThread thread =
                                new NcbiReFetchThread( ns, ac, "", 
                                nativeRestServer, threadRunMinutes, 
                                wsContext );

                            thread.start();

                            log.warn( "getNative: nlm esearch return " +
                                      "an empty set." );
                            log.info( "getNative: nlm esearch thread starting." );

                        } catch ( RuntimeException e ) {
                            if( e.getMessage().equals( "NO_RECORD" ) ) {
                                throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                            } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                            } else {
                                throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                            }
                        }
                    }
           
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT ); 
                } 

                String ncbi_error = xPath.evaluate(
                                            "/eSearchResult/ErrorList" + 
                                            "/PhraseNotFound/text()",  
                                            rootElementEsearch );

                if( !ncbi_error.equals("") ) {
                    log.warn("getNative: nlm esearch: No items found");
                    throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                }

                String ncbi_nlmid = xPath.evaluate( 
                                            "/eSearchResult/IdList/Id/text()", 
                                            rootElementEsearch );

                if( ncbi_nlmid.equals("") ){

                    log.info( "getNative: nlm esearch return wrong xml style. " );
                    
                    if( isRetry ) {
                        try { 
                            NcbiReFetchThread thread =
                                new NcbiReFetchThread( ns, ac, "",
                                nativeRestServer, threadRunMinutes,
                                wsContext );

                            thread.start();

                            log.info( "getNative: nlm: ncbi fetch thread starting... " );

                        } catch ( RuntimeException e ) {
                            if( e.getMessage().equals( "NO_RECORD" ) ) {
                                throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                            } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                            } else if ( e.getMessage().equals( "UNSUPPORTED_OP" ) ) {
                                throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
                            } else {
                                throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                            }
                        }
                    }

                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                }

                //--------------------------------------------------------------                
                // efetch real nlmid 
                //------------------

                log.info( "NcbiServer: nlm: ncbi_nlmid is " + ncbi_nlmid );

                // get native rest servet from context

                String url_efetch_string =
                        nativeRestServer.getRealUrl( provider, "nlmefetch", 
                                                     ncbi_nlmid );

                log.info( "NcbiServer: after replace url_efetch_string=" + 
                          url_efetch_string );

                URL url_efetch = new URL( url_efetch_string );

                InputSource xml_efetch = new InputSource(
                                                url_efetch.openStream() );

                Document docEfetch = builder.parse( xml_efetch );
                Element rootElementEfetch = docEfetch.getDocumentElement();

                Node testNode = (Node) xPath.evaluate(
                               "/NLMCatalogRecordSet/NLMCatalogRecord",
                               rootElementEfetch, XPathConstants.NODE );

                if( testNode == null ) {
                    if( isRetry ) {
                        try { 
                            NcbiReFetchThread thread =
                                new NcbiReFetchThread( ns, ac, ncbi_nlmid,
                                nativeRestServer, threadRunMinutes,
                                wsContext );

                            thread.start();
                            log.info( "getNative: nlm: ncbi fetch thread starting..." );                     

                        } catch ( RuntimeException e ) {
                            if( e.getMessage().equals( "NO_RECORD" ) ) {
                                throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                            } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );            
                            } else {
                                throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                            }
                        }
                    }

                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );

                } else {
                    log.info( "getNative: nlm: testNode != null " +
                              " and ncbi_nlmid=" + ncbi_nlmid + " . " );

                    //***this is old criteria to decide if it's a journal
                    //String publicationType = xPath.evaluate( 
                    //           "/NLMCatalogRecordSet/NLMCatalogRecord/" +
                    //           "PublicationTypeList/" +
                    //           "PublicationType[text()='Periodicals']/text()",
                    //           rootElementEfetch );

                    //if ( publicationType.equals( "" ) ) {
                    //    log.warn( "NcbiServer: nlm: " +
                    //              "PublicationType is not Periodicals\n");
                    
                    String typeOfResource = xPath.evaluate(
                                "/NLMCatalogRecordSet/NLMCatalogRecord" +
                                "/ResourceInfo/TypeOfResource/text()",
                                rootElementEfetch );
                    if( !typeOfResource.equals("Serial") ) {
                        log.warn( "NcbiServer: nlm: " +
                                  "TypeOfResource is not Serial.");
                        throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                    } else {
                        //extract xml string
                        try {

                            record = nativeRestServer.getNative( 
                                                        provider, "nlmefetch",
                                                        ns, ncbi_nlmid, timeout );

                        } catch ( ServerFault fault ) {
                            throw fault;
                        }

                        retVal = record.getNativeXml();

                        if( retVal.trim().equals(
                                "<?xml version=\"1.0\"?><NLMCatalogRecordSet>" + 
                                "</NLMCatalogRecordSet>" ) ) {

                            log.info( "getNative: nlm: retVal is empty set. " );

                            if( isRetry ) {
                                try {    
                                    NcbiReFetchThread thread =
                                        new NcbiReFetchThread( ns, ac, ncbi_nlmid,
                                        nativeRestServer, threadRunMinutes,
                                        wsContext );

                                    thread.start();

                                    log.info( "getNative: ncbi fetch thread starting." );

                                } catch ( RuntimeException e ) {
                                    if( e.getMessage().equals( "NO_RECORD" ) ) {
                                        throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
                                    } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                                        throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                                    } else {
                                        throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
                                    }
                                }
                            }

                            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                        }
                    }
                }
            }catch( ServerFault fault ) {
                throw fault;            
            }catch( Exception e ) {
                log.warn( "NcbiServer: getNative: nlm: " +
                          "getService Exception:\n" + e.toString() + ". ");
                throw ServerFaultFactory.newInstance( Fault.UNKNOWN );
            }
            return record;
        }
    } */
}
