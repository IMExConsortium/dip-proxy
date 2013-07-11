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
import edu.ucla.mbi.proxy.RestServer;
import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.fault.*;

import org.w3c.dom.*;

import javax.xml.xpath.*;
import javax.xml.parsers.*;
import java.net.URL;

public class NcbiGetJournal {

    private String provider = "NCBI";
    private String service = "nlm";

    private RestServer restServer;
    private WSContext wsContext;

    public void setRestServer( RestServer server ) {
        this.restServer = server;
    }

    public void setWsContext( WSContext context ) {
        this.wsContext = context;
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
            Document docEsearch = restServer
                .getNativeDom( provider, "nlmesearch", ac );

            Element rootElementEsearch = docEsearch.getDocumentElement();
            
            if( rootElementEsearch == null 
                || rootElementEsearch.getChildNodes().getLength() ==  0 ) {

                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
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
            
            log.info( "esearch nlmid=" + nlmid );    
            if( nlmid == null || nlmid.equals("") ) {
                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
            }
            return nlmid;

        } catch ( RuntimeException re ) {
            throw re;
        } catch ( Exception e ) {
            log.warn( "nlm esearch exception: " + e.toString() + ". ");
            log.warn( "NcbiGetJournal TERMINATE. " );
            if( isRetry ) {
                NcbiReFetchThread thread =
                        new NcbiReFetchThread( ns, ac, "", timeout,
                                               threadRunMinutes, this,
                                               wsContext);

                thread.start();
            }
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
            
        log.info( "efetch: nlmid is " + nlmid );
            
        try {
            Document docEfetch = restServer
                .getNativeDom( provider, "nlmefetch", nlmid );

            Element rootElementEfetch = docEfetch.getDocumentElement();

            if( rootElementEfetch == null 
                || rootElementEfetch.getChildNodes().getLength() ==  0 ) {

                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
            } 

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xPath = xpf.newXPath();

            log.info( "before evaluate testNode. " );

            Node testNode = (Node) xPath.evaluate(
                "/NLMCatalogRecordSet/NLMCatalogRecord",
                rootElementEfetch, XPathConstants.NODE );

            if( testNode == null ) {
                throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
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
                    record = restServer.getNativeRecord(       
                        provider, "nlmefetch", ns, nlmid, timeout );
                } catch ( ServerFault fault ) {
                    throw fault;
                }

                if( record == null ) {
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                }

                String retVal = record.getNativeXml();

                if( retVal == null || retVal.equals("") 
                    || retVal.trim().equals(
                        "<?xml version=\"1.0\"?><NLMCatalogRecordSet>" +
                        "</NLMCatalogRecordSet>" ) ) {

                    log.info( "retVal is emptySet with= " + retVal );
                    throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
                }
                    
                return record;
            }

        } catch ( RuntimeException re ) {
            throw re;
        } catch ( Exception e ) {
            log.warn( "nlm exception: " + e.toString() + ". ");
            log.info( "NcbiGetJournal TERMINATE. " );

            if( isRetry ) {

                NcbiReFetchThread thread =
                    new NcbiReFetchThread( ns, nlmid, nlmid, timeout,
                                           threadRunMinutes, this, wsContext );

                thread.start();
                log.info( "nlm efetch thread starting." );
            }

            throw new RuntimeException("REMOTE_FAULT");
        } 
    }
}
