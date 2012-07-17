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
                             ) throws ProxyFault {
        return null;
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
                                  ) throws ProxyFault 
    {
	
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
		        //ServiceFault fault = 
		        //new ServiceFault( "05 remote server: transformer." );
		        throw FaultFactory.newInstance( Fault.UNKNOWN ) ; 
	        }	    
	    
	        return dxfResult;
	    
	    } catch ( ProxyFault fault) { 
	        log.info( "Transformer fault:" + fault.getFaultInfo().getMessage() );
	        throw fault;
        } catch ( Exception e ) {
	        log.info( "Exception="+e.toString() );
	        //ServiceFault fault = 
	        //	new ServiceFault( "05 remote server: transform." ); 
	        throw FaultFactory.newInstance( Fault.UNKNOWN );  // 
	    }   
    }
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
				                 String detail, String service, 
				                 ProxyTransformer pTrans 
                                 ) throws ProxyFault 
    {
	
    	// NOTE: overload if dxf building more complex than
	    // a simple xslt transformation
	
	    return this.transform( strNative, ns, ac, detail, service, pTrans );
    }
}

