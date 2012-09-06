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

//public class NcbiServer extends RemoteNativeServer implements NativeServer{
public class NcbiServer extends RemoteServerImpl implements NativeServer{

    private Log log = LogFactory.getLog( NcbiServer.class );
    private NativeRestServer nlmEsearchRestServer = null;
    private NativeRestServer nlmEfetchRestServer = null;
    private NativeRestServer pubmedRestServer = null;
    private NativeRestServer refseqRestServer = null;
    private NativeRestServer entrezgeneRestServer = null;
    private NativeRestServer taxonRestServer = null;

    public void initialize() {

        Log log = LogFactory.getLog( NcbiServer.class );
        log.info( "initialize service" );

        if( getContext() != null ){

            nlmEsearchRestServer = (NativeRestServer) getContext().get(
                                                    "nlmEsearch" );

            nlmEfetchRestServer = (NativeRestServer) getContext().get(
                                                    "nlmEfetch" );

            pubmedRestServer = (NativeRestServer) getContext().get(
                                                   "pubmed" );

            

            refseqRestServer = (NativeRestServer) getContext().get(
                                                    "refseq" );

            entrezgeneRestServer = (NativeRestServer) getContext().get(
                                                    "entrezgene" );

            taxonRestServer = (NativeRestServer) getContext().get(
                                                    "taxon" );

        }
    }

    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeOut ) throws ProxyFault {

        NativeRecord record = null;
        String retVal = null;
        log.info( "NcbiServer: NS=" + ns + " AC=" + ac + " OP=" + service );
            
        if ( service.equals( "nlm" ) ) {

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

                String url_esearch_string = nlmEsearchRestServer.getRestUrl();

                url_esearch_string = url_esearch_string.replaceAll( 
                                    nlmEsearchRestServer.getRestAcTag(), ac );
 
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
                        } else {
                            throw FaultFactory.newInstance( Fault.UNKNOWN );
                        }
                    }
                }

                //--------------------------------------------------------------                
                // efetch real nlmid 
                //------------------

                log.info( "NcbiServer: nlm: ncbi_nlmid is " + ncbi_nlmid );

                String url_efetch_string = nlmEfetchRestServer.getRestUrl();

                log.info( "NcbiServer: nlm: url_efetch_string=" 
                          +  url_efetch_string );

                url_efetch_string = url_efetch_string.replaceAll( 
                            nlmEfetchRestServer.getRestAcTag(), ncbi_nlmid );

                log.info( "NcbiServer: after replace url_efetch_string=" + url_efetch_string );

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
                                                    service, ns, ncbi_nlmid, timeOut );
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
        }

        if ( service.equals( "pubmed" ) ) {
            if( pubmedRestServer == null ) {
                log.warn( "getNative: pubmedRestServer is null. " );
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            }

            try {
                record = ((NativeRestServer) 
                    getContext().get(service)).getNative( provider, service,
                                                          ns, ac, timeOut );
            
            //                                    "pubmedRestServer" );
            //                                       "pubmed"
            //record = pubmedRestServer.getNative( provider, service, 
            //                                         ns, ac, timeOut );
            } catch ( ProxyFault fault ) {
                throw fault;
            } 
            
        }
        
        if ( service.equals( "refseq" ) ) {
            if( refseqRestServer == null ) {
                log.warn( "getNative: refseqRestServer is null. " );
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            }

            try {
                record = refseqRestServer.getNative( provider, service, 
                                                     ns, ac, timeOut );
            } catch ( ProxyFault fault ) {
                throw fault;
            }

            retVal = record.getNativeXml();

            if( retVal.contains("<INSDSet><Error>") 
                    || retVal.contains( "<TSeqSet/>" ) ) {

                log.warn( "NcbiServer: refseq get wrong retVal for ac " + 
                          ac + "." );
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            } 
        }
        
        if ( service.equals( "entrezgene" ) ) {
            if( entrezgeneRestServer == null ) {
                log.warn( "getNative: entrezgeneRestServer is null. " );
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            }

            try {
                record = entrezgeneRestServer.getNative( provider, service, 
                                                         ns, ac, timeOut );
            } catch ( ProxyFault fault ) {
                throw fault;
            }

        }
        
        if ( service.equals( "taxon" ) ) {
            if( taxonRestServer == null ) {
                log.warn( "getNative: taxonRestServer is null. " );
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            }

            try {
                record = taxonRestServer.getNative( provider, service,
                                                    ns, ac, timeOut );
            } catch ( ProxyFault fault ) {
                throw fault;
            }
        }

        return record;   
    }
}
