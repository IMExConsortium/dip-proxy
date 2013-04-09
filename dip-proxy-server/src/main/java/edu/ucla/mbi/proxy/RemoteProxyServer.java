package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * RemoteProxyServer:
 *
 *    returns string representation of a data record requested from the 
 *    server using ns/ac (namespace/accession) pair as identifier and
 *    operation as the remote service name
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;

import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBContext;

import javax.xml.transform.stream.StreamSource;
import javax.xml.datatype.XMLGregorianCalendar;

import javax.xml.ws.Holder;
import javax.xml.ws.BindingProvider;
import com.sun.xml.ws.developer.JAXWSProperties;

import edu.ucla.mbi.dxf14.DatasetType;
import edu.ucla.mbi.dxf14.DxfJAXBContext;
import edu.ucla.mbi.fault.*;

import edu.ucla.mbi.cache.NativeRecord;

import java.util.Map;

public class RemoteProxyServer implements NativeServer {

    private Log log = LogFactory.getLog( RemoteProxyServer.class ); 
    
    private String proxyAddress;
    private Map<String,Object> context;
   
    public String getAddress(){
        return proxyAddress;
    }
    
    public void setAddress( String url ) {
        url = url.replaceAll("^\\s+","");
        url = url.replaceAll("\\s+$","");
        
        this.proxyAddress = url;
    }

    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }
    
    public Map<String,Object> getContext() {
	    return context;
    }
   
    public void initialize() {
        log.info( "initialize service=" + this );
    }

    public RemoteProxyServer getRemoteProxyServerInstance( String url) {
        return new RemoteProxyServer( url );
    }

    public RemoteProxyServer(){};

    public RemoteProxyServer( String url ){
        this.setAddress( url );
    };


    public NativeRecord getNative( String provider, String service,
                                   String ns, String ac, int timeout 
                                   ) throws ProxyFault {

        log.info( "getNative(NS=" + ns + " AC=" + ac + " OP=" + service + ")" );

        String url = getAddress();

        ProxyService proxySrv = new ProxyService();
        ProxyPort port = proxySrv.getProxyPort();

        try {
            log.info( " this=" + this );
            log.info( " port=" + url + " timeout=" + timeout );

            // set server location
            // ---------------------

            ((BindingProvider) port).getRequestContext().put(
                    BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url );

            // set client Timeout
            // ------------------

            ((BindingProvider) port).getRequestContext().put(
                   JAXWSProperties.CONNECT_TIMEOUT, timeout );
        
            Holder<DatasetType> resDataset = new Holder<DatasetType>();
            Holder<String> resNative = new Holder<String>();
            Holder<XMLGregorianCalendar> timestamp =
                                new Holder<XMLGregorianCalendar>();

            port.getRecord( provider, service, ns, ac, "", "", "native", 
                                 "", 0, timestamp, resDataset, resNative );

            String resultStr = resNative.value;
            XMLGregorianCalendar qtime = timestamp.value;

            NativeRecord record = new NativeRecord( provider, service, ns, ac );
            record.setNativeXml( resNative.value );

            record.setQueryTime( qtime.toGregorianCalendar().getTime() );

            return record;

        } catch ( ProxyFault fault ) {
            throw fault;
        } catch ( Exception e ) {
            log.info( "getNative: exception: " + e.toString() );
            if ( e.toString().contains( "No result found" ) ) {
                throw FaultFactory.newInstance( Fault.NO_RECORD ); // no hits
            } else if ( e.toString().contains( "Read timed out" ) ) {
                throw FaultFactory.newInstance( Fault.REMOTE_TIMEOUT ); // timeout
            } else if( e.toString().contains( 
                        "ConnectException: Connection refused" ) ) {
                throw FaultFactory.newInstance( Fault.REMOTE_FAULT ); //remote down
            } else {
                throw FaultFactory.newInstance( Fault.UNKNOWN ); // unknown
            }
        }
    }
}

