package edu.ucla.mbi.client;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProxyServiceClient:
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
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.soap.*;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProxyServiceClient {

    private static ProxyService service =  new ProxyService();
    private static ProxyPort port = service.getProxyPort();

    private static String serviceURL;
    private static int timeOutMilliSecond;
    private static int proxyReQueryTimes;

    private static Log log = LogFactory.getLog( ProxyServiceClient.class );

    //constructor
    public ProxyServiceClient(  String serviceURL,
                                int timeOutMinute,
                                int proxyReQueryTimes ) {

        initialize( serviceURL, timeOutMinute, proxyReQueryTimes );

    }

    public static void initialize ( String serviceURLIn,
                                    int timeOutMinuteIn,
                                    int proxyReQueryTimesIn ) {

        serviceURL = serviceURLIn.replaceAll( "^\\s*", "" );
        serviceURL = serviceURL.replaceAll( "\\s*$", "" );

        timeOutMilliSecond = timeOutMinuteIn * 60000;
        proxyReQueryTimes = proxyReQueryTimesIn;

        log.info( "initialize:ebiServiceURL=" + serviceURL );
    }

    //*** used for outside calling
    public static NodeType getFilteredPicrRecord ( String ns, String ac,
                                                   String ncbiTaxonId
                                                   ) throws ProxyFault {

        NodeType node =  getRecordWithDxf( "EBI", "picr", ns, ac, "full" );
        
        if( !ncbiTaxonId.equals( "" ) ) {
            NodeType nodeR = picrFilter ( node, ncbiTaxonId );
            return nodeR;
        } else {
            return node;
        }
    }

    //*** used for outside calling
    public static NodeType getRecordWithDxf( String provider, String service,
                                             String ns, String ac, String detail 
                                             ) throws ProxyFault {

        return getRecordWithDxf( provider, service, ns, ac, detail, 0 );
    }

    //*** used for inside calling 
    private static NodeType getRecordWithDxf( String provider, String service, 
                                              String ns, String ac, 
                                              String detail, int testNum 
                                              ) throws ProxyFault {

        NodeType node = null;

        // set server location
        // ---------------------

        ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceURL );

        // set client Timeout
        // ------------------
        ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeOutMilliSecond );

        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp =
                                new Holder<XMLGregorianCalendar>();

            String resultStr;

            port.getRecord( provider, service, ns, ac, "", detail, "dxf", "", 
                            0, timestamp, resDataset, resNative );

            edu.ucla.mbi.dxf14.ObjectFactory dof =
                                new edu.ucla.mbi.dxf14.ObjectFactory();
            
            node = resDataset.value.getNode().get(0);

            while( node == null && ++testNum < proxyReQueryTimes ) {
                Holder<DatasetType> resDataset1 = new Holder<DatasetType>();
                Holder<String> resNative1 = new Holder<String>();
                Holder<XMLGregorianCalendar>
                    timestamp1 = new Holder<XMLGregorianCalendar>();

                port.getRecord ( provider, service, ns, ac, "", detail, "dxf", 
                                 "", 0, timestamp1, resDataset1, resNative1 );

                node = resDataset1.value.getNode().get(0);
            }
    
            if( node == null){
                String message = "getRecord: remote server returns null.";
                ServiceFault sf = new ServiceFault();
                sf.setMessage(message);
                sf.setFaultCode(13);
                log.warn( "getRecord: throw fault: " +
                          "remote server returns null.");
                ProxyFault fault = new ProxyFault(message, sf);
                throw fault;
            }
        } catch ( ProxyFault fault ) {
            if( ++testNum < proxyReQueryTimes ) {
                getRecordWithDxf ( provider, service, ns, ac, detail, testNum );
            }
            throw fault;
        }

        return node;

    }

    private static NodeType picrFilter ( NodeType node, 
                                         String ncbiTaxonId 
                                         ) throws ProxyFault {

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
            if( xref.getNs().matches( "uniprot|refseq" )
                && xref.getNode().getType().getName().equals( "protein" ) ) {

                String taxId = xref.getNode().getXrefList()
                                    .getXref().get(0).getAc();

                if( taxId.equals("-3") ){
                    log.info( "picrFilter: taxId=-3 with xerf.getNs="
                              + xref.getNs() + "." );

                    NodeType nodeUR = null;

                    try {
                        if( xref.getNs().equals("uniprot") ) {
                            nodeUR = getRecordWithDxf ( 
                                                "EBI", "uniprot", "uniprot", 
                                                xref.getAc(), "base" );

                        }

                        if( xref.getNs().equals("refseq") ) {
                            nodeUR = getRecordWithDxf (
                                                "NCBI", "refseq", "refseq",
                                                xref.getAc(), "base" );

                        }
                    } catch ( ProxyFault fault ) {
                        log.warn( "picrFilter: throw fault " +
                                      "from getRecordWithDxf.");
                            throw fault;
                    } catch ( Exception e ) {
                        String message = "picrFilter: got an exception. ";

                        ServiceFault sf = new ServiceFault();
                        sf.setMessage(message);
                        sf.setFaultCode( 99 );

                        log.warn( "picrFilter: throw exception: using "
                                  + "getRecordWithDxf." );

                        ProxyFault fault = new ProxyFault( message, sf );
                        throw fault;
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
                            String message = "picrFilter: getRefseq or " 
                                             + "getUniprot return null xref " 
                                             + "list for the node with taxid=-3.";

                            ServiceFault sf = new ServiceFault();
                            sf.setMessage(message);
                            sf.setFaultCode(5);
                            log.warn( "picrFilter: throw fault: getRefseq " 
                                      + "or getUniprot return null xref for " 
                                      + "the node with taxid=-3." );
                            ProxyFault fault = new ProxyFault(message, sf);
                            throw fault;
                        }
                    }else {
                        String message = "picrFilter: getRefseq or " 
                                         + "getUniprot return null for " 
                                         + "the node with taxid=-3.";

                        ServiceFault sf = new ServiceFault();
                        sf.setMessage(message);
                        sf.setFaultCode(5);
                        log.warn( "picrFilter: throw fault: getRefseq or " 
                                  + "getUniprot return null for the node " 
                                  + "with taxid=-3." );

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

    
    public static void main( java.lang.String[] args ) {

        String sr = args[0]; // service URL
        String provider = args[1]; // provider
        String service = args[2];  // service

        int timeout = 300 * 1000; // client TimeOut 5 mins
        String ac = "";
        String ns = "";
        String detail = "stub";
        String format = "dxf";

        for ( int i = 3; i < args.length; i++ ) {
            if ( args[i].startsWith( "AC=" ) ) {
                ac = args[i].replaceFirst( "AC=", "" );
            }
            
            if ( args[i].startsWith( "NS=" ) ) {
                ns = args[i].replaceFirst( "NS=", "" );
            }

            
            if ( args[i].startsWith( "DETAIL=" ) ) {
                detail = args[i].replaceFirst( "DETAIL=", "" );
            }

            if ( args[i].startsWith( "FORMAT=" ) ) {
                format = args[i].replaceFirst( "FORMAT=", "" );
            }

        }

        System.out.println( "SRV: " + sr + " ->Provider: " + provider 
                            + " ->service: " + service 
                            + " -> AC=" + ac
                            + " -> NS=" + ac
                            + " -> FORMAT=" + format 
                            + " -> DETAIL=" + detail );

        try {
            
            ProxyService proxyService = new ProxyService();
            ProxyPort port = proxyService.getProxyPort();
            

            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sr );

            // set client Timeout
            // ------------------

            ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeout );

            System.out.println( "Proxy: ProxyService=" + service 
                                + " port=" + port );

            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp = 
                                new Holder<XMLGregorianCalendar>();

            String resultStr;

            port.getRecord( provider, service, ns, ac, "", detail, format, "", 0,
                            timestamp, resDataset, resNative );

            edu.ucla.mbi.dxf14.ObjectFactory dof = 
                                new edu.ucla.mbi.dxf14.ObjectFactory();

            if( format.equalsIgnoreCase( "dxf" ) 
                    || format.equalsIgnoreCase( "both" ) ) 
            {
                DatasetType dataset = resDataset.value;

                JAXBContext jc = DxfJAXBContext.getDxfContext();

                Marshaller marshaller = jc.createMarshaller();
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
                                    new Boolean( true ) );

                java.io.StringWriter sw = new StringWriter();
                marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );

                marshaller.marshal( dof.createDataset( dataset ), sw );

                resultStr = sw.toString();
                
                System.out.println( "DXF Result:" );
                System.out.println( resultStr );
            } 

            if ( format.equalsIgnoreCase( "native" ) 
                    || format.equalsIgnoreCase( "both" ) ) 
            {
                resultStr = resNative.value;
                System.out.println( "NATIVE Result:" );
                System.out.println( resultStr );
            } 

        } catch ( Exception e ) {
            System.out.println( e.toString() );
        }
    }
    
    
}
