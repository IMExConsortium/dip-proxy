package edu.ucla.mbi.client;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * Category:
 *
 *=========================================================================== */

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.services.*;

import javax.xml.bind.*;
import java.io.*;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import com.sun.xml.ws.developer.JAXWSProperties;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EbiProxyClient {

    private static EbiProxyService service =  new EbiProxyService();
    private static EbiProxyPort port = service.getProxyPort();

    private static String ebiServiceURL;
    private static int timeOutMilliSecond;
    private static int proxyReQueryTimes;

    private static Log log = LogFactory.getLog( EbiProxyClient.class );

    //constructor
    public EbiProxyClient( String ebiServiceURL,
                           int timeOutMinute,
                           int proxyReQueryTimes ) {

        initialize( ebiServiceURL, timeOutMinute, proxyReQueryTimes );

    }

    public static void initialize ( String ebiServiceURLIn,
                                    int timeOutMinuteIn,
                                    int proxyReQueryTimesIn ) {

        ebiServiceURL = DipTrimURL.trim( ebiServiceURLIn );
        timeOutMilliSecond = timeOutMinuteIn * 60000;
        proxyReQueryTimes = proxyReQueryTimesIn;

        log.info( "initialize:ebiServiceURL=" + ebiServiceURL );
    }

    public static NodeType getUniprot( String ac, String detail, int testNum ) 
                                                        throws ProxyFault {

        ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ebiServiceURL );
        ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeOutMilliSecond );

        try {

            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> 
                timestamp = new Holder<XMLGregorianCalendar>();

            port.getUniprot( "uniprot", ac, "", detail, "dxf", "", 0, 
                             timestamp, resDataset, resNative );

            NodeType node = resDataset.value.getNode().get(0);
            
            while(node == null && testNum < proxyReQueryTimes){
                testNum++;
                Holder<DatasetType> resDataset1 = new Holder<DatasetType>();
                Holder<String> resNative1 = new Holder<String>();
                Holder<XMLGregorianCalendar> 
                    timestamp1 = new Holder<XMLGregorianCalendar>();

                port.getUniprot( "uniprot", ac, "", detail, "dxf", "", 0, 
                                 timestamp1, resDataset1, resNative1 );
                node = resDataset1.value.getNode().get(0);
            }
            
            if( node == null){
                String message = "getUniprot: remote server returns null.";
                ServiceFault sf = new ServiceFault();
                sf.setMessage(message);
                sf.setFaultCode(13);
                log.warn( "getUniprot: throw fault: " + 
                          "remote server returns null.");
                ProxyFault fault = new ProxyFault(message, sf);
                throw fault;
            }
            return node;
        } catch ( ProxyFault fault ) {
            if( testNum < proxyReQueryTimes ){
                getUniprot( ac, detail, ++testNum);
            }
            throw fault;
        }
    }

    public static NodeType getPicrList( String ns, String ac, String detail, 
                                        int testNum, boolean filter, 
                                        String ncbiTaxonId 
                                        ) throws ProxyFault { 

        NodeType node = null;

        ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ebiServiceURL );
        ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeOutMilliSecond );

        if( filter ){
            detail = "full";
        }

        log.info( "getPicrList: ac=" + ac + " filter=" + filter + 
                  " ncbiTaxonId=" + ncbiTaxonId );
        
        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> 
                timestamp = new Holder<XMLGregorianCalendar>();

            port.getPicrList( ns, ac, "", detail, "", "", 0, timestamp, 
                              resDataset, resNative );

            node = resDataset.value.getNode().get(0);

            while( ( node == null || node.getXrefList() == null) 
                   && testNum < proxyReQueryTimes ) {

                testNum++;
                Holder<DatasetType> resDataset1 = new Holder<DatasetType>();
                Holder<String> resNative1 = new Holder<String>();
                Holder<XMLGregorianCalendar> 
                    timestamp1 = new Holder<XMLGregorianCalendar>();
                port.getPicrList( ns, ac, "", detail, "", "", 0, timestamp1, 
                                  resDataset1, resNative1 );
                node = resDataset1.value.getNode().get(0);
            }
            
         }catch (ProxyFault fault ){
            ServiceFault sf = (ServiceFault)fault.getFaultInfo();
            if( sf.getFaultCode() == 5 ){
                //*** no record found
                return null;
            }   
 
            if( testNum < proxyReQueryTimes ){
                getPicrList( ns, ac, detail, ++testNum, filter, ncbiTaxonId );
            }else{
                if( sf.getMessage().contains(
                        "Unsupported Content-Type: text/html") ) {

                    String message = "getPicrList: remote server " + 
                                     "return wrong content type.";
                    ServiceFault proxySf = new ServiceFault();
                    proxySf.setMessage(message);
                    proxySf.setFaultCode(13);
                    log.warn( "getPicrList: throw fault: remote server " + 
                              "returns wrong content type." );
                    ProxyFault proxyFault = new ProxyFault(message, proxySf);  
                    throw proxyFault;  
                }else{
                    throw fault;
                }
            }
        } 

        if( node == null || node.getXrefList() == null ) { 
            String message = "getPicrList: remote server returns null.";
            ServiceFault sf = new ServiceFault();
            sf.setMessage(message);
            sf.setFaultCode(13);
            log.warn("getPicrList: throw fault: remote server returns null.");
            ProxyFault fault = new ProxyFault(message, sf);
            throw fault;
        }

        if( filter ){
            NodeType nodeR = picrFilter ( node, ncbiTaxonId );
            return nodeR;
        }

        return node;

    }

    private static NodeType picrFilter ( NodeType node, String ncbiTaxonId ) 
                                                            throws ProxyFault {
        
        log.info("picrFilter: ncbiTaxonId=" + ncbiTaxonId);
    
        if( ncbiTaxonId == null || ncbiTaxonId.equals("0") ) {
            String message = "EbiProxyClient: picrFilter: invalid ncbiTaxonId.";
            ServiceFault sf = new ServiceFault();
            sf.setMessage(message);
            sf.setFaultCode(3);
            log.warn("picrFilter: throw fault: invalid ncbiTaxonId.");
            ProxyFault fault = new ProxyFault(message, sf);
            throw fault;
        }

        if( node == null || node.getXrefList() == null ) {
            String message = "picrFilter: input node cannot be null.";
            ServiceFault sf = new ServiceFault();
            sf.setMessage(message);
            sf.setFaultCode(4);
            log.warn( "picrFitler: throw fault: input node can't be null." );
            ProxyFault fault = new ProxyFault( message, sf );
            throw fault;
        }

        NodeType.XrefList xrefList = node.getXrefList();
        edu.ucla.mbi.dxf14.ObjectFactory 
            dxfOF = new edu.ucla.mbi.dxf14.ObjectFactory();
            
        node.setXrefList(dxfOF.createNodeTypeXrefList());
        NodeType.XrefList xrefListNew = node.getXrefList();
        
        log.info ( "picrFilter: xrefList.getXref().size=" 
                   + xrefList.getXref().size() + "." );

        for( int i = 0; i < xrefList.getXref().size(); i++ ) {
            XrefType xref = xrefList.getXref().get(i);
            if( xref.getNs().matches("(uniprot|refseq)") 
                && xref.getNode().getType().getName().equals("protein") ) {

                String taxId = xref.getNode().getXrefList()
                                    .getXref().get(0).getAc();                            

                if( taxId.equals("-3") ){
                    log.info( "picrFilter: taxId=-3 with xerf.getNs=" 
                              + xref.getNs() + "." ); 

                    NodeType nodeUR = null;
                    if( xref.getNs().equals("uniprot") ) {
                        nodeUR = getUniprot(xref.getAc(), "base", 0 );
                    } 

                    if( xref.getNs().equals("refseq")){
                        try{
                            nodeUR = NcbiProxyClient.getRefseq (
                                            xref.getAc(), "base", 0 );

                        } catch ( ProxyFault fault ) {
                            log.warn( "picrFilter: throw fault " + 
                                      "from ncbiProxyClient getRefseq.");
                            throw fault;
                        } catch ( Exception e ) {
                            String message = "picrFilter: getRefseq got " 
                                             + "an exception. "; 

                            ServiceFault sf = new ServiceFault();
                            sf.setMessage(message);
                            sf.setFaultCode( 99 );
                        
                            log.warn( "picrFilter: throw exception: using " 
                                      + "ncbiProxyClient getRefseq." );
 
                            ProxyFault fault = new ProxyFault( message, sf );
                            throw fault;
                        }
                    } 
                    
                    if( nodeUR != null ) {
                        NodeType.XrefList xrefListUR = nodeUR.getXrefList();
                        if( xrefListUR != null ) {
                            for( int j = 0; 
                                 j < xrefListUR.getXref().size(); 
                                 j++ ) {

                                XrefType xrefUR = xrefListUR.getXref().get(j);
                                log.info( "picrFitler: xrefUR.getNs()=" 
                                           + xrefUR.getNs() + "." );
                                if( xrefUR.getNs().equals("ncbitaxid") ) {
                                    taxId = xrefUR.getAc();
                                    break;
                                }
                            }
                        }else {
                            String message = "picrFilter: getRefseq or " + 
                                             "getUniprot return null xref " + 
                                             "list for the node with taxid=-3.";

                            ServiceFault sf = new ServiceFault();
                            sf.setMessage(message);
                            sf.setFaultCode(5);
                            log.warn( "picrFilter: throw fault: getRefseq " + 
                                      "or getUniprot return null xref for " + 
                                      "the node with taxid=-3." );
                            ProxyFault fault = new ProxyFault(message, sf);
                            throw fault;
                        }
                    }else {
                        String message = "picrFilter: getRefseq or " +
                                         "getUniprot return null for " + 
                                         "the node with taxid=-3.";

                        ServiceFault sf = new ServiceFault();
                        sf.setMessage(message);
                        sf.setFaultCode(5);
                        log.warn( "picrFilter: throw fault: getRefseq or " + 
                                  "getUniprot return null for the node " + 
                                  "with taxid=-3." );
                        ProxyFault fault = new ProxyFault( message, sf );
                        throw fault;
                    }
    
                } 
                        
                if( taxId.equals( ncbiTaxonId ) ) {
                    xref.setNode(null);
                    xrefListNew.getXref().add(xref);
                }
            }
        }

        log.info ( "picrFilter: xrefListNew.getXref().size="
                   + xrefListNew.getXref().size() + "." );

        node.setXrefList( xrefListNew );
        
        return node;
    }   
}
