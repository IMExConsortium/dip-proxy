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

import edu.ucla.mbi.services.Fault;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import java.io.StringBufferInputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.net.URL;

public class NcbiServer extends RemoteNativeServer {

    private Log log = LogFactory.getLog( NcbiServer.class );

    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeOut ) throws ProxyFault {

        String retVal = null;
        log.info( "NcbiServer: NS=" + ns + " AC=" + ac + " OP=" + service );
            
        if ( service.equals( "nlm" ) ) {

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xPath = xpf.newXPath();
            DocumentBuilderFactory fct = DocumentBuilderFactory.newInstance();

            //------------------------------------------------------------------
            // esearch ncbi internal id of the nlmid
            //--------------------------------------
            
            String url_esearch_string =
                "http://eutils.ncbi.nlm.nih.gov"
                + "/entrez/eutils/esearch.fcgi"
                + "?db=nlmcatalog&retmode=xml&term=" + ac + "[nlmid]";

            try{
                DocumentBuilder builder = fct.newDocumentBuilder();

                URL url_esearch = new URL( url_esearch_string );
                InputSource xml_esearch = new InputSource( 
                                                url_esearch.openStream() );

                Document docEsearch = builder.parse( xml_esearch );
                Element rootElementEsearch = docEsearch.getDocumentElement();
             
                if( rootElementEsearch.getChildNodes().getLength() ==  0 ) {
                    log.warn("getNative: nlm esearch: return an empty result." ); 
                    NcbiReFetchThread thread = new NcbiReFetchThread(
                                                            ns, ac, "" );
                    thread.start();
                    log.info( "getNative: nlm: ncbi fetch thread starting... " );

                    throw FaultFactory.newInstance( Fault.REMOTE_FAULT ); // REMOTE_FAULT
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
                    
                    NcbiReFetchThread thread = new NcbiReFetchThread( ns, ac, "" );
                    thread.start();
                    log.info( "getNative: nlm: ncbi fetch thread starting... " );

                    throw FaultFactory.newInstance( Fault.REMOTE_FAULT ); // REMOTE_FAULT 
                }

                //--------------------------------------------------------------                
                // efetch real nlmid 
                //------------------

                log.info( "NcbiServer: nlm: ncbi_nlmid is " + ncbi_nlmid );
                
                String url_efetch_string =
                    "http://eutils.ncbi.nlm.nih.gov"
                    + "/entrez/eutils/efetch.fcgi?db=nlmcatalog&retmode=xml&id="
                    + ncbi_nlmid ;

                URL url_efetch = new URL( url_efetch_string );
                InputSource xml_efetch = new InputSource(
                                                url_efetch.openStream() );

                Document docEfetch = builder.parse( xml_efetch );
                Element rootElementEfetch = docEfetch.getDocumentElement();

                Node testNode = (Node) xPath.evaluate(
                               "/NLMCatalogRecordSet/NLMCatalogRecord",
                               rootElementEfetch, XPathConstants.NODE );

                if( testNode == null ) {
                    log.warn("getNative: nlm: native server return empty set. ");
                   
                    NcbiReFetchThread thread = new NcbiReFetchThread( 
                                                    ns, ac, ncbi_nlmid );
                    thread.start();

                    log.info( "getNative: nlm: ncbi fetch thread starting..." );                     
                    throw FaultFactory.newInstance( Fault.REMOTE_FAULT );  
                } else {
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
                        retVal = NativeURL.query( url_efetch_string, timeOut );
                        if( retVal == null || retVal.trim().equals(
                                "<?xml version=\"1.0\"?><NLMCatalogRecordSet>" + 
                                "</NLMCatalogRecordSet>" ) ) {

                            log.info( "getNative: nlm: retVal is empty set. " );
                            NcbiReFetchThread thread = new NcbiReFetchThread(
                                                            ns, ac, ncbi_nlmid );
                            thread.start();
                            log.info( "getNative: ncbi fetch thread starting." );
                            throw FaultFactory.newInstance( Fault.REMOTE_FAULT ); 
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
            String url =
                "http://eutils.ncbi.nlm.nih.gov"
                + "/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id="
                + ac;
            retVal = NativeURL.query( url, timeOut );
        }
        
        if ( service.equals( "refseq" ) ) {
            String url =
                "http://eutils.ncbi.nlm.nih.gov"
                + "/entrez/eutils/efetch.fcgi?db=protein&id=" + ac
                + "&rettype=gpc&retmode=xml";

            retVal = NativeURL.query( url, timeOut );
            
            if( retVal.contains("<INSDSet><Error>") 
                    || retVal.contains( "<TSeqSet/>" ) ) {

                log.warn( "NcbiServer: refseq get wrong retVal for ac " + 
                          ac + "." );
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
            } 
        }
        
        if ( service.equals( "entrezgene" ) ) {
            String url =
                "http://eutils.ncbi.nlm.nih.gov"
                + "/entrez/eutils/efetch.fcgi?db=gene&id=" + ac
                + "&retmode=xml";
            retVal = NativeURL.query( url, timeOut );
        }
        
        if ( service.equals( "taxon" ) ) {
            String url =
                    "http://eutils.ncbi.nlm.nih.gov"
                            + "/entrez/eutils/efetch.fcgi?db=taxonomy&id=" + ac
                            + "&retmode=xml";
            retVal = NativeURL.query( url, timeOut );
        }

        if ( retVal == null ){
            log.warn( "NcbiServer: get retVal null or not found for ac " + ac + "." );
            throw FaultFactory.newInstance( Fault.NO_RECORD );
        }

        NativeRecord record = new NativeRecord( provider, service, ns, ac);
        record.setNativeXml( retVal );
        return record;   
    }
}
