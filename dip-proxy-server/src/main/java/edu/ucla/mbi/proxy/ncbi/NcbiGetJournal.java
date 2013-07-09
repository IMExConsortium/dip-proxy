package edu.ucla.mbi.proxy.ncbi;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NcbiGetJournal:
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.context.WSContext;
import edu.ucla.mbi.proxy.NativeRestServer;
import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.cache.orm.NativeRecordDAO;
import edu.ucla.mbi.fault.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.xpath.*;
import javax.xml.parsers.*;
import java.net.URL;

public class NcbiGetJournal {

    private String provider = "NCBI";
    private String service = "nlm";

    private WSContext wsContext;
    private NativeRestServer nativeRestServer;

    public void setWsContext ( WSContext context ) {
        this.wsContext = context;
    }

    public void setNativeRestServer( NativeRestServer server ) {
        this.nativeRestServer = server;
    }
    
    public void initialize() {
        Log log = LogFactory.getLog( NcbiGetJournal.class );
        log.info( "NcbiGetJournal initailize() called ..." );
    }

    //--------------------------------------------------------------------------
    // esearch ncbi internal id of the nlmid
    //--------------------------------------------------------------------------

    public String esearch ( String ns, String ac, int threadRunMinutes, 
        boolean isRetry ) throws RuntimeException {
   
        Log log = LogFactory.getLog( NcbiGetJournal.class );
 
        // XPath to retrieve the content
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPath = xpf.newXPath();
        DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();
        
        NativeRecord record = null;
        NativeRecordDAO nativeRecordDAO =
            wsContext.getDipProxyDAO().getNativeRecordDAO();

        try {
            String url_esearch_string =
                nativeRestServer.getRealUrl( provider, "nlmesearch", ac );

            DocumentBuilder builder = fct.newDocumentBuilder();

            URL url_esearch = new URL( url_esearch_string );
            InputSource xml_esearch = 
                new InputSource( url_esearch.openStream() );

            Document docEsearch = builder.parse( xml_esearch );
            Element rootElementEsearch = docEsearch.getDocumentElement();

            if( rootElementEsearch == null 
                || rootElementEsearch.getChildNodes().getLength() ==  0 ) {

                log.info( "nlm esearch get empty return." );

                if( isRetry ) {
                
                    NcbiReFetchThread thread =
                        new NcbiReFetchThread( ns, ac, "", 
                                               threadRunMinutes, this );

                    thread.start();

                    log.warn( "nlm esearch return an empty set." );
                    log.info( "nlm esearch thread starting." );
                }
                throw new RuntimeException( "REMOTE_FAULT" );
            }    
                
            String ncbi_error = (String) xPath.evaluate(
                "/eSearchResult/ErrorList/" +
                "PhraseNotFound/text()", rootElementEsearch );

            if( !ncbi_error.equals("")){
                log.warn("nlm esearch: No items found");
                throw new RuntimeException( "NO_RECORD" );
            }

            String nlmid = (String) xPath.evaluate(
                "/eSearchResult/IdList/Id/text()", rootElementEsearch);
                
            return nlmid;

        } catch ( RuntimeException re ) {
            throw re;
        } catch ( Exception e ) {
            log.warn( "nlm exception: " + e.toString() + ". ");
            log.warn( "NcbiGetJournal TERMINATE. " );
            throw new RuntimeException("REMOTE_FAULT");
        } 
    }

    //--------------------------------------------------------------------------                
    // efetch real nlmid 
    //--------------------------------------------------------------------------
    public NativeRecord efetch ( String ns, String ac, String nlmid, 
        int threadRunMinutes, boolean isRetry ) throws RuntimeException {

        Log log = LogFactory.getLog( NcbiGetJournal.class );

        if( nlmid.equals( "" ) ) {
            if( isRetry ) {
                NcbiReFetchThread thread =
                    new NcbiReFetchThread( ns, ac, "",
                                           threadRunMinutes, this );

                thread.start();

                log.warn( "nlm esearch return an empty set." );
                log.info( "nlm esearch thread starting." );
            }

            ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }
            
        log.info( "nlmid is " + nlmid );
            
        boolean emptySet = true;

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPath = xpf.newXPath();

        DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();

        try {
            String url_efetch_string =
                nativeRestServer.getRealUrl( provider, "nlmefetch", nlmid );

            URL url_efetch = new URL( url_efetch_string );
            InputSource xml_efetch = new InputSource( url_efetch.openStream() );

            DocumentBuilder builder = fct.newDocumentBuilder();
            Document docEfetch = builder.parse( xml_efetch );
            Element rootElementEfetch = docEfetch.getDocumentElement();

            Node testNode = (Node) xPath.evaluate(
                "/NLMCatalogRecordSet/NLMCatalogRecord",
                rootElementEfetch, XPathConstants.NODE );

            if( testNode == null ) {
                if( isRetry ) {
                    NcbiReFetchThread thread =
                        new NcbiReFetchThread( ns, ac, nlmid,
                                               threadRunMinutes, this );

                    thread.start();

                    log.warn( "getNative: nlm efetch return an empty set." );
                    log.info( "getNative: nlm efetch thread starting." );
                }

                throw new RuntimeException("REMOTE_FAULT");
            }
                
            String typeOfResource = xPath.evaluate(
                "/NLMCatalogRecordSet/NLMCatalogRecord" +
                "/ResourceInfo/TypeOfResource/text()", rootElementEfetch );
                        
            if( !typeOfResource.equals("Serial") ) {
                log.warn( "nlm: TypeOfResource is not Serial.");
                throw new RuntimeException("NO_RECORD");
            } else {

                NativeRecord record = null;

                try {
                    record = nativeRestServer.getNative(
                        provider, "nlmefetch", ns,
                        nlmid, wsContext.getTimeout( "NCBI" ) );
                } catch ( ServerFault fault ) {
                    throw new RuntimeException("REMOTE_FAULT");
                }

                if( record == null ) {
                    return null;
                }

                String retVal = record.getNativeXml();

                if( !retVal.trim().equals(
                    "<?xml version=\"1.0\"?><NLMCatalogRecordSet>" +
                    "</NLMCatalogRecordSet>" ) ) {

                    emptySet = false;

                    if( isRetry ) {

                        NcbiReFetchThread thread =
                            new NcbiReFetchThread( ns, ac, nlmid,
                                                   threadRunMinutes, this );

                        thread.start();

                        log.warn( "nlm efetch return an empty set." );
                        log.info( "nlm efetch thread starting." );
                    }   
                    throw new RuntimeException("REMOTE_FAULT");
                }
                    
                return record;
            }
        } catch ( RuntimeException re ) {
            throw re;
        } catch ( Exception e ) {
            log.warn( "nlm exception: " + e.toString() + ". ");
            log.info( "NcbiGetJournal TERMINATE. " );

            throw new RuntimeException("REMOTE_FAULT");
        } 
    }
}
