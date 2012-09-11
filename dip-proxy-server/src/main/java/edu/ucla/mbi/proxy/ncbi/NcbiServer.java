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

public class NcbiServer extends RemoteServerImpl {

    private Log log = LogFactory.getLog( NcbiServer.class );
     
    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeout ) throws ProxyFault {

        log.info( "NcbiServer: NS=" + ns + " AC=" + ac + " OP=" + service );
    
        if ( !service.equals( "nlm" ) ) {
            return super.getNative( provider, service, ns, ac, timeout );
        } else {
            NativeRecord record = null;
            String retVal = null;
            NativeRestServer nlmEsearchRestServer = 
                    (NativeRestServer) getContext().get( "nlmEsearch" );

            NativeRestServer nlmEfetchRestServer = 
                    (NativeRestServer) getContext().get( "nlmEfetch" );

            if( nlmEsearchRestServer == null || nlmEfetchRestServer == null ) {
                log.warn( "getNative: nlmEsearchRestServer "
                          + "or nlmEfetchRestServer is null. " );

                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            }

            
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xPath = xpf.newXPath();
            DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();

            //------------------------------------------------------------------
            // esearch ncbi internal id of the nlmid
            //--------------------------------------
            
            try{
                DocumentBuilder builder = fct.newDocumentBuilder();

                String url_esearch_string = (String)nlmEsearchRestServer
                                    .getRestServerContext().get( "restUrl" );
                String esearch_restAcTag = (String)nlmEsearchRestServer
                                    .getRestServerContext().get( "restAcTag" );
            
                if( esearch_restAcTag == null || url_esearch_string == null ) {
                    log.warn( "getNative: esearch_restAcTag or " + 
                              "url_esearch_string is not configured. " );
                    throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
                }


                url_esearch_string = url_esearch_string.replaceAll( 
                                                    esearch_restAcTag, ac );
 
                URL url_esearch = new URL( url_esearch_string );
                
                log.info( "getNative: url_esearch=" + url_esearch ); 
                InputSource xml_esearch = new InputSource( 
                                                url_esearch.openStream() );

                Document docEsearch = builder.parse( xml_esearch );
                Element rootElementEsearch = docEsearch.getDocumentElement();
             
                if( rootElementEsearch.getChildNodes().getLength() ==  0 ) {
                    log.warn("getNative: nlm esearch: return an empty result." ); 
                    
                    try {    
                        NcbiReFetchThread thread = new NcbiReFetchThread(
                                                        ns, ac, "", 
                                                        nlmEsearchRestServer,
                                                        nlmEfetchRestServer);

                        thread.start();
                        log.info( "getNative: nlm: ncbi fetch thread starting... " );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    } catch ( ProxyFault fault ) {
                        throw fault;
                    } catch ( RuntimeException e ) {
                        if( e.getMessage().equals( "NO_RECORD" ) ) {
                            throw FaultFactory.newInstance( Fault.NO_RECORD );
                        } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                        } else {
                            throw FaultFactory.newInstance( Fault.UNKNOWN );
                        }
                    }
                }

                String ncbi_error = xPath.evaluate(
                                            "/eSearchResult/ErrorList" + 
                                            "/PhraseNotFound/text()",  
                                            rootElementEsearch );

                if( !ncbi_error.equals("") ) {
                    log.warn("getNative: nlm esearch: No items found");
                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                }

                String ncbi_nlmid = xPath.evaluate( 
                                            "/eSearchResult/IdList/Id/text()", 
                                            rootElementEsearch );

                if( ncbi_nlmid.equals("") ){
                    log.warn("getNative: nlm esearch: return wrong xml style. ");
                  
                    try { 
                        NcbiReFetchThread thread = new NcbiReFetchThread( 
                                                        ns, ac, "",
                                                        nlmEsearchRestServer,
                                                        nlmEfetchRestServer);

                        thread.start();

                        log.info( "getNative: nlm: ncbi fetch thread starting... " );
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    } catch ( ProxyFault fault ) {
                        throw fault;
                    } catch ( RuntimeException e ) {
                        if( e.getMessage().equals( "NO_RECORD" ) ) {
                            throw FaultFactory.newInstance( Fault.NO_RECORD );
                        } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                        } else if ( e.getMessage().equals( "UNSUPPORTED_OP" ) ) {
                            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
                        } else {
                            throw FaultFactory.newInstance( Fault.UNKNOWN );
                        }
                    }
                }

                //--------------------------------------------------------------                
                // efetch real nlmid 
                //------------------

                log.info( "NcbiServer: nlm: ncbi_nlmid is " + ncbi_nlmid );

                String url_efetch_string = (String)nlmEfetchRestServer
                                    .getRestServerContext().get( "restUrl" );

                String efetch_restAcTag = (String)nlmEfetchRestServer
                                    .getRestServerContext().get( "restAcTag" );

                
                if( efetch_restAcTag == null || url_efetch_string == null ) {
                    log.warn( "getNative: efetch_restAcTag or " +
                              "url_efetch_string is not configured. " );
                    throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );
                }

                url_efetch_string = url_efetch_string.replaceAll( 
                                            efetch_restAcTag, ncbi_nlmid );

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
                    log.warn( "getNative: nlm: native server return empty set. " );
                    try { 
                        NcbiReFetchThread thread = new NcbiReFetchThread( 
                                                        ns, ac, ncbi_nlmid,
                                                        nlmEsearchRestServer,
                                                        nlmEfetchRestServer);

                        thread.start();

                        log.info( "getNative: nlm: ncbi fetch thread starting..." );                     
                        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                    } catch ( ProxyFault fault ) {
                        throw fault;
                    } catch ( RuntimeException e ) {
                        if( e.getMessage().equals( "NO_RECORD" ) ) {
                            throw FaultFactory.newInstance( Fault.NO_RECORD );
                        } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );            
                        } else {
                            throw FaultFactory.newInstance( Fault.UNKNOWN );
                        }
                    }
                } else {
                    log.info( "getNative: nlm: testNode != null " +
                              " and ncbi_nlmid=" + ncbi_nlmid + " . " );
                    /*
                    //this is old criteria to decide if it's a journal
                    String publicationType = xPath.evaluate( 
                               "/NLMCatalogRecordSet/NLMCatalogRecord/" +
                               "PublicationTypeList/" +
                               "PublicationType[text()='Periodicals']/text()",
                               rootElementEfetch );

                    if ( publicationType.equals( "" ) ) {
                        log.warn( "NcbiServer: nlm: " +
                                  "PublicationType is not Periodicals\n");
                    */
                    String typeOfResource = xPath.evaluate(
                                "/NLMCatalogRecordSet/NLMCatalogRecord" +
                                "/ResourceInfo/TypeOfResource/text()",
                                rootElementEfetch );
                    if( !typeOfResource.equals("Serial") ) {
                        log.warn( "NcbiServer: nlm: " +
                                  "TypeOfResource is not Serial.");
                        throw FaultFactory.newInstance( Fault.NO_RECORD );
                    } else {
                        //extract xml string
                        try {
                            record = nlmEfetchRestServer.getNative( provider, 
                                                    service, ns, ncbi_nlmid, timeout );
                        } catch ( ProxyFault fault ) {
                            throw fault;
                        }

                        retVal = record.getNativeXml();

                        if( retVal.trim().equals(
                                "<?xml version=\"1.0\"?><NLMCatalogRecordSet>" + 
                                "</NLMCatalogRecordSet>" ) ) {

                            log.info( "getNative: nlm: retVal is empty set. " );
                            
                            try {    
                                NcbiReFetchThread thread 
                                    = new NcbiReFetchThread ( 
                                                        ns, ac, ncbi_nlmid,
                                                        nlmEsearchRestServer,
                                                        nlmEfetchRestServer);

                                thread.start();

                                log.info( "getNative: ncbi fetch thread starting." );

                                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );

                            } catch ( ProxyFault fault ) {
                                throw fault;
                            } catch ( RuntimeException e ) {
                                if( e.getMessage().equals( "NO_RECORD" ) ) {
                                    throw FaultFactory.newInstance( Fault.NO_RECORD );
                                } else if ( e.getMessage().equals( "REMOTE_FAULT" ) ) {
                                    throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
                                } else {
                                    throw FaultFactory.newInstance( Fault.UNKNOWN );
                                }
                            }
                        }
                    }
                }
            }catch( ProxyFault fault ) {
                throw fault;            
            }catch( Exception e ) {
                log.warn( "NcbiServer: getNative: nlm: " +
                          "getService Exception:\n" + e.toString() + ". ");
                throw FaultFactory.newInstance( Fault.UNKNOWN );
            }
            return record;
        } 
    }
}
