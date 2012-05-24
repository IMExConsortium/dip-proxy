package edu.ucla.mbi.proxy.ncbi;

/*==============================================================================
 * $HeadURL$
 * $Id$
 * Version: $Rev$
 *==============================================================================
 *
 * NcbiReFetchThread:
 *    in case NCBI web service returns empty set, the thread will refetch again
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceException;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import java.io.StringBufferInputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.cache.orm.*;
import java.util.*;
import java.net.URL;

public class NcbiReFetchThread extends Thread {

    private Log log = LogFactory.getLog( NcbiReFetchThread.class );
    private String ns, ac;
    private String nlmid = "";
    private int ttl = WSContext.getServerContext("NCBI").getTtl();
    private int timeOut = WSContext.getServerContext("NCBI").getTimeout();
    private int threadRunMinutes = WSContext.getThreadRunMinutes();
    private long waitMillis = threadRunMinutes * 60 * 1000;  
    private String provider = "NCBI";
    private String service = "nlm";
    
    public NcbiReFetchThread( String ns, String ac, String nlmid ) {
        this.ns = ns;
        this.ac = ac;
        this.nlmid = nlmid;
    }

    public void run() {
        log.info( "NcbiFetchThread running... " ); 
        String retVal = null;

        // XPath to retrieve the content
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPath = xpf.newXPath();
        DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();
        long startTime = System.currentTimeMillis();
        
        if( nlmid.equals( "" ) ) {
            log.info( "NcbiReFetchThread: nlmid is empty. " );
            while ( System.currentTimeMillis() - startTime < waitMillis ) {

                //------------------------------------------------------------------
                // esearch ncbi internal id of the nlmid
                //--------------------------------------
            
                String url_esearch_string =
                    "http://eutils.ncbi.nlm.nih.gov"
                    + "/entrez/eutils/esearch.fcgi"
                    + "?db=nlmcatalog&retmode=xml&term=" + ac + "[nlmid]";

                try {
                    DocumentBuilder builder = fct.newDocumentBuilder();

                    URL url_esearch = new URL( url_esearch_string );
                    InputSource xml_esearch = new InputSource(
                                                url_esearch.openStream() );

                    Document docEsearch = builder.parse( xml_esearch );
                    Element rootElementEsearch = docEsearch.getDocumentElement();
                    
                    if( rootElementEsearch.getChildNodes().getLength() >  0 ) {
                        String ncbi_error = (String) xPath.evaluate(
                                                "/eSearchResult/ErrorList/" +
                                                "PhraseNotFound/text()", 
                                                rootElementEsearch );

                        if( !ncbi_error.equals("")){
                            log.warn("getNative: nlm esearch: No items found");
                            break;
                        }
                
                        nlmid = (String) xPath.evaluate( 
                                            "/eSearchResult/IdList/Id/text()", 
                                            rootElementEsearch);

                        if( !nlmid.equals("") ){
                            break;
                        }
                    }    
                } catch ( Exception e ) {
                    log.warn( "getNative: nlm: " +
                              "getService Exception:\n" + e.toString() + ". ");
                    log.warn( "NcbiReFetchThread TERMINATE. " );
                    return;           
                }
            }
        }
                
        //--------------------------------------------------------------                
        // efetch real nlmid 
        //------------------

        if( !nlmid.equals( "" ) ) {
            log.info( "NcbiReFetchThread: after esearch: nlmid is " + nlmid );
            startTime = System.currentTimeMillis();            
            boolean emptySet = true;
            String url_efetch_string = "http://eutils.ncbi.nlm.nih.gov"
                        + "/entrez/eutils/efetch.fcgi?db=nlmcatalog&retmode=xml&id="
                        + nlmid ;

            while ( System.currentTimeMillis() - startTime < waitMillis ) {

                try { 
                    URL url_efetch = new URL( url_efetch_string );
                    InputSource xml_efetch = new InputSource(
                                                url_efetch.openStream() );

                    DocumentBuilder builder = fct.newDocumentBuilder();
                    Document docEfetch = builder.parse( xml_efetch );
                    Element rootElementEfetch = docEfetch.getDocumentElement();

                    Node testNode = (Node) xPath.evaluate(
                                        "/NLMCatalogRecordSet/NLMCatalogRecord",
                                        rootElementEfetch, XPathConstants.NODE );
 
                    if( testNode != null ) {
                        String typeOfResource = xPath.evaluate(
                                "/NLMCatalogRecordSet/NLMCatalogRecord" +
                                "/ResourceInfo/TypeOfResource/text()",
                                rootElementEfetch );
                        if( !typeOfResource.equals("Serial") ) {
                            log.warn( "NcbiServer: nlm: " +
                                  "TypeOfResource is not Serial.");
                            return;
                        } else {
                            retVal = NativeURL.query( url_efetch_string, timeOut );

                            if( !retVal.trim().equals(
                                "<?xml version=\"1.0\"?><NLMCatalogRecordSet>" + 
                                "</NLMCatalogRecordSet>" ) ) {

                                emptySet = false;
                                break;
                            }
                        }
                    }
                } catch ( Exception e ) {
                    log.warn( "NcbiReFetchThread: getNative: nlm: " +
                              "thread Exception:\n" + e.toString() + ". ");
                    log.info( "NcbiReFetchThread: TERMINATE. " );
                   
                    return;                   
                }
            }

            if( !emptySet && retVal != null ) {
                
                NativeRecord record = new NativeRecord( provider, service, 
                                                            ns, ac);
                record.setNativeXml( retVal );

                NativeRecordDAO nDAO = DipProxyDAO.getNativeRecordDAO(); 
                   
                synchronized( this ) { 
                    NativeRecord cacheRecord = nDAO.find( provider, service, ns, ac );
                    
                    Date remoteQueryTime = record.getCreateTime();

                    if ( cacheRecord == null ) { // local record does not present
                        record.resetExpireTime( remoteQueryTime, ttl );
                        nDAO.create( record ); // store/update native record locally
                        log.info( "NcbiReFetchThread: getNative: new native record is created.");
                    } else {
                        Date expirationTime = cacheRecord.getExpireTime();
                        Date currentTime = Calendar.getInstance().getTime();
                        log.info( "NcbiReFetchThread: Native record with ac=" + ac +
                                  ": CT=" + cacheRecord.getCreateTime() +
                                  " ET=" + expirationTime );

                        if ( currentTime.after( expirationTime ) ) {
                            cacheRecord.setNativeXml ( retVal );
                            cacheRecord.resetExpireTime ( remoteQueryTime, ttl );
                            nDAO.create( record ); // store/update native record locally
                            log.info( "NcbiReFetchThread: getNative: native record is updated.");
                        }
                    }
                }
            }
        }

        log.info( "NcbiReFetchThread: final DONE. " );
    }
}
