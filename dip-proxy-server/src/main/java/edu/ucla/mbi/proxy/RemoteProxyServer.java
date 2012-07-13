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

public class RemoteProxyServer implements RemoteServer {
 
    String proxyAddress;

    private static Map<String,Object> context;

    public boolean isNative(){
        return false;
    }

    public String getAddress(){
        return proxyAddress;
    }

    public void setAddress( String url ) {
	    url = url.replaceAll("^\\s+","");
	    url = url.replaceAll("\\s+$","");
	
	    this.proxyAddress=url;
    }
    
    public void setContext( Map<String,Object> context ) {
        RemoteProxyServer.context = context;
    }
    
    public Map<String,Object> getContext() {
	return context;
    }
    
    public NativeRecord getNative( String provider, String service,
                                   String ns, String ac, int timeout
                                   ) throws ProxyFault 
    {
        Log log = LogFactory.getLog( RemoteProxyServer.class );

        log.info( "getNative(NS=" + ns + " AC=" + ac + " OP=" + service + ")" );

        // call EBI proxy localized at ProxyAddress

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

            port.getRecord( provider, service, ns, ac, "", "", "native", "", 0,
                            timestamp, resDataset, resNative );

            XMLGregorianCalendar qtime = timestamp.value;

            NativeRecord record = new NativeRecord( provider, service, ns, ac );
            record.setNativeXml( resNative.value );
            record.setCreateTime( qtime.toGregorianCalendar().getTime() );

            return record;

        } catch ( ProxyFault fault ) {
            throw fault;
        } catch ( Exception e ) {
            log.info( "getNative: exception: " + e.toString() );
            if ( e.toString().contains( "No result found" ) ) {
                throw FaultFactory.newInstance( Fault.NO_RECORD ); // no hits
            } else if ( e.toString().contains( "Read timed out" ) ) {
                throw FaultFactory.newInstance( Fault.REMOTE_TIMEOUT ); // timeout
            } else {
                throw FaultFactory.newInstance( Fault.UNKNOWN ); // unknown
            }
        }
    }

    public RemoteServer getRemoteServerInstance() {
        return null;
    }

    public RemoteProxyServer getRemoteProxyServerInstance() {
        return null;
    }
    

    public void initialize() {
	    Log log = LogFactory.getLog( RemoteServer.class );
	    log.info("Initializing: " + this );
    }


    public DatasetType transform( String strNative,
				                  String ns, String ac, String detail,
				                  String service, ProxyTransformer pTrans 
                                  ) throws ProxyFault {
	
        Log log = LogFactory.getLog( RemoteProxyServer.class );
	    try {
	        // native data in string representationa as input
	    
	        ByteArrayInputStream bisNative =
		    new ByteArrayInputStream( strNative.getBytes( "UTF-8" ) );
	        StreamSource ssNative = new StreamSource( bisNative );
	    
	        // dxf as JAXBResult result of the transformation
	    
	        JAXBContext dxfJc = DxfJAXBContext.getDxfContext();
	        // JAXBContext.newInstance( "edu.ucla.mbi.dxf14" );
	        JAXBResult result = new JAXBResult( dxfJc );
	    
	        //transform into DXF
	    
	        pTrans.setTransformer( service );
    	    pTrans.setParameters( detail, ns, ac );
    	    pTrans.transform( ssNative, result );
	    
	        DatasetType dxfResult  = 
		    (DatasetType) ( (JAXBElement) result.getResult() ).getValue();
	    
            //test if dxfResult is empty
	        if ( dxfResult.getNode().isEmpty() ) {
		        throw FaultFactory.newInstance( Fault.TRANSFORM ); 
	        }	    
	    
	        return dxfResult;
	    
	    } catch ( ProxyFault fault ) { 
	        log.info( "Transformer fault: empty dxfResult ");
	        throw fault;
        } catch ( Exception e ) {
	        log.info( "Exception="+e.toString() );
	        throw FaultFactory.newInstance( Fault.TRANSFORM );  
	    
	    }   
    }
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
				                 String detail, String service, 
				                 ProxyTransformer pTrans 
                                 ) throws ProxyFault {
	
    	// NOTE: overload if dxf building more complex than
	    //       a simple xslt transformation
	
	    return this.transform( strNative, ns, ac, detail, service, pTrans );
    }
}

