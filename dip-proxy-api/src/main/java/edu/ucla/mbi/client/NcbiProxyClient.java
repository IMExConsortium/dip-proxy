package edu.ucla.mbi.client;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NcbiServiceClient:
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

public class NcbiProxyClient {
    
    private static NcbiProxyService service = new NcbiProxyService();
    private static NcbiProxyPort port = service.getProxyPort();
    
    private static String serviceURL;
    private static int timeOutMilliSecond;
    private static int proxyReQueryTimes;

    private static Log log = LogFactory.getLog( NcbiProxyClient.class );

    //constructor
    public NcbiProxyClient( String serviceURL, 
                            int timeOutMinute, 
                            int proxyReQueryTimes ) {

        initialize( serviceURL, timeOutMinute, proxyReQueryTimes );
    }

    public static void initialize( String serviceURLIn,
                                   int timeOutMinuteIn,
                                   int proxyReQueryTimesIn ) {

        serviceURL = DipTrimURL.trim( serviceURLIn );
        timeOutMilliSecond = timeOutMinuteIn * 60000;
        proxyReQueryTimes = proxyReQueryTimesIn;

        log.info( "initialize:serviceURL=" + serviceURL );
    }

    public static NodeType getTaxon( String ac, String detail, int testNum ) 
                                                        throws ProxyFault {

        
        ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceURL );

        ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeOutMilliSecond );

        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> 
                timestamp = new Holder<XMLGregorianCalendar>();

            port.getTaxon( "ncbitaxid", ac, "", detail, "dxf", "", 0, timestamp,
                           resDataset, resNative );

            NodeType node = resDataset.value.getNode().get(0);
            
            while(node == null && testNum < proxyReQueryTimes){
                testNum++;
                Holder<DatasetType> resDataset1 = new Holder<DatasetType>();
                Holder<String> resNative1 = new Holder<String>();
                Holder<XMLGregorianCalendar> 
                    timestamp1 = new Holder<XMLGregorianCalendar>();

                port.getTaxon( "ncbitaxid", ac, "", detail, "dxf", "", 0, 
                               timestamp1, resDataset1, resNative1 );
                node = resDataset1.value.getNode().get(0);
            }    
            
            if( node == null ){
                String message = "NcbiProxyClient: getTaxon: " + 
                                 "remote server returns null.";
                ServiceFault sf = new ServiceFault();
                sf.setMessage(message);
                sf.setFaultCode(13);
                log.warn("getTaxon: throw fault: remote server returns null.");
                ProxyFault fault = new ProxyFault(message, sf);
                throw fault;
            }
 

            return node;
        }catch(ProxyFault proxyFault){
            if( testNum < proxyReQueryTimes ){
                getTaxon( ac, detail, ++testNum );
            }
            String faultMessage = proxyFault.getFaultInfo().getMessage(); 
            throw proxyFault;
        }
    }

    public static NodeType getRefseq( String ac, String detail, int testNum )
                                                        throws ProxyFault {

        ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceURL );

        ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeOutMilliSecond );
        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> 
                timestamp = new Holder<XMLGregorianCalendar>();

            port.getRefseq( "refseq", ac, "", detail, "dxf", "", 0, timestamp,
                            resDataset, resNative );

            NodeType node = resDataset.value.getNode().get(0);
            
            while( node == null && testNum < proxyReQueryTimes ) {
                testNum++;
                Holder<DatasetType> resDataset1 = new Holder<DatasetType>();
                Holder<String> resNative1 = new Holder<String>();
                Holder<XMLGregorianCalendar> 
                    timestamp1 = new Holder<XMLGregorianCalendar>();
                port.getRefseq( "refseq", ac, "", detail, "dxf", "", 0, timestamp1,
                                resDataset1, resNative1 );
                node = resDataset1.value.getNode().get(0);
            }
            
            if( node == null ){
                String message = "NcbiProxyClient: getRefseq: " + 
                                 "remote server returns null.";
                ServiceFault sf = new ServiceFault();
                sf.setMessage(message);
                sf.setFaultCode(13);
                log.warn( "getRefseq: throw fault: " + 
                          "remote server returns null." );
                ProxyFault fault = new ProxyFault(message, sf);
                throw fault;
            }
            return node;
        } catch ( ProxyFault e ) {
            if( testNum < proxyReQueryTimes ){
                getRefseq( ac, detail, ++testNum );
            }
            throw e;
        }
    }

    public static NodeType getPubmedArticle ( String ac, 
                                       String detail, 
                                       int testNum ) throws ProxyFault {

        ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, serviceURL );

        ((BindingProvider) port).getRequestContext().put(
                    JAXWSProperties.CONNECT_TIMEOUT, timeOutMilliSecond );
        try {
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> 
                timestamp = new Holder<XMLGregorianCalendar>();

            port.getPubmedArticle( "pubmed", ac, "", detail, "dxf", "", 0, 
                                   timestamp, resDataset, resNative );

            NodeType node = resDataset.value.getNode().get(0);
      
             
            while( node == null && testNum < proxyReQueryTimes ) {
                testNum++;
                Holder<DatasetType> resDataset1 = new Holder<DatasetType>();
                Holder<String> resNative1 = new Holder<String>();
                Holder<XMLGregorianCalendar> 
                    timestamp1 = new Holder<XMLGregorianCalendar>();

                port.getPubmedArticle( "pubmed", ac, "", detail, "dxf", "", 0, 
                                       timestamp1, resDataset1, resNative1 );
                node = resDataset1.value.getNode().get(0);
            } 
            
            if( node == null ){
                String message = "getPubmedArticle: " + 
                                 "remote server returns null.";
                ServiceFault sf = new ServiceFault();
                sf.setMessage(message);
                sf.setFaultCode(13);
                log.warn( "getPubmedArticle: throw fault: " + 
                          "remote server returns null." );
                ProxyFault fault = new ProxyFault(message, sf);
                throw fault;
            }

            return node;
        }catch(ProxyFault proxyFault){
            if( testNum < proxyReQueryTimes ){
                getPubmedArticle( ac, detail, ++testNum );
            }
            String faultMessage = proxyFault.getFaultInfo().getMessage();
            throw proxyFault;
        }
    }

}
