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

    private final String PROVIDER = "NCBI";
    private final String SERVICE = "nlm";
    private final String NS = "nlmid";

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
    
    public String esearch( String ac ) throws ServerFault {

        String nlmid = null;
        
        Log log = LogFactory.getLog( NcbiGetJournal.class );
        
        Document docEsearch = restServer
            .getNativeDom( PROVIDER, "nlmesearch", NS, ac );
        
        Element rootElemEsearch = docEsearch.getDocumentElement();
        
        if( rootElemEsearch == null 
            || rootElemEsearch.getChildNodes().getLength() ==  0 ) {
            
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        } 
        
        try {
            XPathFactory xpf = XPathFactory.newInstance();
            XPath xPath = xpf.newXPath();
            
            String ncbi_error = (String) xPath
                .evaluate( "/eSearchResult/ErrorList/" +
                           "PhraseNotFound/text()", rootElemEsearch );
            
            if( !ncbi_error.equals("")){
                log.warn("nlm esearch: No items found");
                throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
            }
        
            nlmid = (String) xPath
                .evaluate( "/eSearchResult/IdList/Id/text()", rootElemEsearch);
            
        } catch ( XPathExpressionException xpf ){
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        }
        
        log.info( "esearch nlmid=" + nlmid );    
        if( nlmid == null || nlmid.equals("") ) {
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        }
        return nlmid;
    }

    //--------------------------------------------------------------------------                
    // efetch real nlmid 
    //--------------------------------------------------------------------------
    
    public NativeRecord efetch ( String ns, String nlmid, int timeout ) 
        throws ServerFault {

        Log log = LogFactory.getLog( NcbiGetJournal.class );
        
        if( nlmid.equals( "" ) ) {
            ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
        }
            
        log.info( "efetch: nlmid is " + nlmid );
            
        Document docEfetch = restServer
            .getNativeDom( PROVIDER, SERVICE, NS, nlmid );

        Element rootElementEfetch = docEfetch.getDocumentElement();

        if( rootElementEfetch == null 
            || rootElementEfetch.getChildNodes().getLength() ==  0 ) {

            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        } 

        try {
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
                throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
            } 
        } catch ( XPathExpressionException xpf ){
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        NativeRecord record = null;
            
        record = restServer.getNativeRecord( 
            PROVIDER, SERVICE, ns, nlmid, timeout );

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
}














