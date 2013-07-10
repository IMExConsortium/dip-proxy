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
import edu.ucla.mbi.fault.*;

import org.w3c.dom.*;

import javax.xml.xpath.*;
import javax.xml.parsers.*;
import java.net.URL;

public class NcbiGetJournal {

    private String provider = "NCBI";
    private String service = "nlm";

    private NativeRestServer nativeRestServer;

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

    public String esearch ( String ns, String ac, int timeout, 
                            int threadRunMinutes, boolean isRetry  
                            ) throws RuntimeException {
   
        Log log = LogFactory.getLog( NcbiGetJournal.class );
         
        try {
            Document docEsearch = nativeRestServer
                .getNativeDom( provider, service, ns, ac, timeout );

            Element rootElementEsearch = docEsearch.getDocumentElement();

            if( rootElementEsearch == null 
                || rootElementEsearch.getChildNodes().getLength() ==  0 ) {

                log.info( "nlm esearch get empty return." );

                if( isRetry ) {
                
                    NcbiReFetchThread thread =
                        new NcbiReFetchThread( ns, ac, "", timeout, 
                                               threadRunMinutes, this );

                    thread.start();

                    log.warn( "nlm esearch return an empty set." );
                    log.info( "nlm esearch thread starting." );
                }
                throw new RuntimeException( "REMOTE_FAULT" );
            }    
                
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xPath = xpf.newXPath();

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
    public NativeRecord efetch ( String ns, String nlmid, int timeout, 
        int threadRunMinutes, boolean isRetry ) throws RuntimeException {

        Log log = LogFactory.getLog( NcbiGetJournal.class );

        if( nlmid.equals( "" ) ) {
            ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }
            
        log.info( "nlmid is " + nlmid );
            
        boolean emptySet = true;

        try {
            Document docEfetch = nativeRestServer
                .getNativeDom( provider, service, ns, nlmid, timeout );

            Element rootElementEfetch = docEfetch.getDocumentElement();

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xPath = xpf.newXPath();

            Node testNode = (Node) xPath.evaluate(
                "/NLMCatalogRecordSet/NLMCatalogRecord",
                rootElementEfetch, XPathConstants.NODE );

            if( testNode == null ) {
                if( isRetry ) {
                    NcbiReFetchThread thread =
                        new NcbiReFetchThread( ns, nlmid, nlmid, timeout,
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
                    record = nativeRestServer.getNativeRecord(
                        provider, "nlmefetch", ns, nlmid, timeout );
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
                            new NcbiReFetchThread( ns, nlmid, nlmid, timeout,
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
