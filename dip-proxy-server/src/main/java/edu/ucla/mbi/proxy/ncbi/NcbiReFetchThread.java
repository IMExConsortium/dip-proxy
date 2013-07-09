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

import edu.ucla.mbi.proxy.context.WSContext;
import edu.ucla.mbi.proxy.NativeRestServer;
import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.cache.orm.*;
import java.util.*;

public class NcbiReFetchThread extends Thread {

    private Log log = LogFactory.getLog( NcbiReFetchThread.class );
    private String ns, ac;
    private String nlmid = "";

    private int ttl;
    private int timeOut;
    private int threadRunMinutes;
    private long waitMillis; 

    private String provider = "NCBI";
    private String service = "nlm";
    private WSContext wsContext = null;
    private NcbiGetJournal ncbiGetJournal = null;
    
    //private NativeRestServer nativeRestServer = null;

    /*
    public NcbiReFetchThread( String ns, String ac, String nlmid,
                              NativeRestServer nativeRestServer,
                              int threadRunMinutes, WSContext wsContext ) {
    */
    public NcbiReFetchThread( String ns, String ac, String nlmid,
                              int threadRunMinutes, 
                              NcbiGetJournal ncbiGetJournal ) {
        this.ns = ns;
        this.ac = ac;
        this.nlmid = nlmid;
        this.threadRunMinutes = threadRunMinutes;
        this.waitMillis = threadRunMinutes * 60 * 1000;
        this.ncbiGetJournal = ncbiGetJournal;
        this.wsContext = wsContext;

        //this.nativeRestServer = nativeRestServer;
        //this.ttl = wsContext.getTtl( provider ); 
        //this.timeOut = wsContext.getTimeout( provider );
    }

    public void run() {
        log.info( "NcbiFetchThread running... " ); 
        String retVal = null;
        /*
        // XPath to retrieve the content
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xPath = xpf.newXPath();
        DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();
        */
        NativeRecord record = null;
        NativeRecordDAO nativeRecordDAO = 
            wsContext.getDipProxyDAO().getNativeRecordDAO();
        

        long startTime = System.currentTimeMillis();

        if( ! wsContext.isDbCacheOn( "NCBI" ) ) return;     
        if( ns == null || ac == null ) return;

        if( nlmid.equals( "" ) ) {
            log.info( "NcbiReFetchThread: nlmid is empty. " );
            while ( System.currentTimeMillis() - startTime < waitMillis ) {

                //--------------------------------------------------------------
                // esearch ncbi internal id of the nlmid
                nlmid = ncbiGetJournal.esearch( ns, ac, 
                                                threadRunMinutes, false );

                if( !nlmid.equals("") ){
                    break;
                }

                /*
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
                            throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
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
                }*/
            }
        }
                
        //----------------------------------------------------------------------                
        // efetch real nlmid 
        //------------------

        if( !nlmid.equals( "" ) ) {
            log.info( "NcbiReFetchThread: after esearch: nlmid is " + nlmid );
            startTime = System.currentTimeMillis();            
            boolean emptySet = true;

            while ( System.currentTimeMillis() - startTime < waitMillis ) {
                record = ncbiGetJournal.efetch( ns, ac, nlmid, 
                                                threadRunMinutes, false );
                if( record != null ) {
                    break;
                }
            }
                /*
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
                                    provider, "nlmefetch", ns,
                                    nlmid, wsContext.getTimeout( "NCBI" ) );

                            } catch ( ServerFault fault ) {
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
            }*/

            //if( !emptySet && retVal != null ) {
            if( record != null ) {
    
                //record.setNativeXml( retVal );

                synchronized( nativeRecordDAO ) {
                    try {
                       
                        NativeRecord cacheRecord = nativeRecordDAO
                            .find( provider, service, ns, ac );
                        
                        if( cacheRecord != null ) {
                            record.setId( cacheRecord.getId() );
                            record.setCreateTime( cacheRecord.getCreateTime() );
                        }

                        record.resetExpireTime( wsContext.getTtl( "NCBI" ) );

                        nativeRecordDAO.create( record );
                        
                        log.info( "NcbiReFetchThread: getNative: native record " +
                              "is create/updated.");

                    } catch ( Exception ex ) {
                        throw new RuntimeException( "TRANSACTION" ) ;
                    }
                }

            }
        }

        log.info( "NcbiReFetchThread: final DONE. " );
    }
}
