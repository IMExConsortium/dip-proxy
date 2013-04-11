package edu.ucla.mbi.proxy.ncbi;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NcbiReFetchThread:
 *    in case NCBI web service returns empty set, the thread will refetch again
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.server.WSContext;
import edu.ucla.mbi.proxy.NativeRestServer;
import edu.ucla.mbi.proxy.ProxyFault;
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

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.cache.orm.*;
import java.util.*;
import java.net.URL;

public class NcbiReFetchThread extends Thread {

    private Log log = LogFactory.getLog( NcbiReFetchThread.class );
    private String ns, ac;
    private String nlmid = "";
    private Map<String, Object> context;

    /*
    private int ttl = WSContext.getServerContext("NCBI").getTtl();
    private int timeOut = WSContext.getServerContext("NCBI").getTimeout();
    private int threadRunMinutes = WSContext.getThreadRunMinutes();
    private long waitMillis = threadRunMinutes * 60 * 1000;
    */
    private int ttl;
    private int timeOut;
    private long waitMillis;  

    private String provider = "NCBI";
    private String service = "nlm";
    private NativeRestServer nativeRestServer = null;

    public NcbiReFetchThread( String ns, String ac, String nlmid, 
                              NativeRestServer nativeRestServer,
                              Map<String, Object> context ) {
        this.ns = ns;
        this.ac = ac;
        this.nlmid = nlmid;
        this.nativeRestServer = nativeRestServer;
        this.context = context;
        this.ttl = Integer.parseInt( (String)context.get( "ttl" ) );
        this.timeOut = Integer.parseInt( (String)context.get("timeout" ) );
        this.waitMillis = Integer.parseInt( 
            (String)context.get("threadRunMinutes") ) * 60 * 1000;
    }

    public void run() {
        log.info( "NcbiFetchThread running... " ); 
        String retVal = null;

        // XPath to retrieve the content
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPath = xpf.newXPath();
        DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();
        long startTime = System.currentTimeMillis();
        NativeRecord record = null;
 
        if( nlmid.equals( "" ) ) {
            log.info( "NcbiReFetchThread: nlmid is empty. " );
            while ( System.currentTimeMillis() - startTime < waitMillis ) {

                //------------------------------------------------------------------
                // esearch ncbi internal id of the nlmid
                //--------------------------------------
                try {
                    String url_esearch_string =
                        nativeRestServer.getRealUrl( provider, "nlmesearch", ac );
        
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
                            throw FaultFactory.newInstance( Fault.NO_RECORD );
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
                    throw new RuntimeException("REMOTE_FAULT");          
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

            while ( System.currentTimeMillis() - startTime < waitMillis ) {

                try { 
                    String url_efetch_string =
                        nativeRestServer.getRealUrl( provider, "nlmefetch", nlmid );

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
                            throw new RuntimeException("NO_RECORD");
                        } else {

                            try {
                                record = nativeRestServer.getNative(
                                                        provider, "nlmefetch",
                                                        ns, nlmid, timeOut );

                            } catch ( ProxyFault fault ) {
                                throw fault;
                            }

                            retVal = record.getNativeXml();

                            if( !retVal.trim().equals(
                                "<?xml version=\"1.0\"?><NLMCatalogRecordSet>" + 
                                "</NLMCatalogRecordSet>" ) ) {

                                emptySet = false;
                                break;
                            }
                        }
                    }
                } catch ( RuntimeException e ) {
                    throw e;            
                } catch ( Exception e ) {
                    log.warn( "NcbiReFetchThread: getNative: nlm: " +
                              "thread Exception:\n" + e.toString() + ". ");
                    log.info( "NcbiReFetchThread: TERMINATE. " );
                   
                    throw new RuntimeException("REMOTE_FAULT");                  
                }
            }

            if( !emptySet && retVal != null ) {
                
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
                            nDAO.create( cacheRecord ); // store/update native record locally
                            log.info( "NcbiReFetchThread: getNative: native record is updated.");
                        }
                    }
                }
            }
        }

        log.info( "NcbiReFetchThread: final DONE. " );
    }
}
