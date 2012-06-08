package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * RemoteServer:
 *
 *    returns string representation of a data record requested from the 
 *    server using ns/ac (namespace/accession) pair as identifier and
 *    operation as the remote service name
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;

import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBContext;

import javax.xml.transform.stream.StreamSource;

import edu.ucla.mbi.dxf14.DatasetType;
import edu.ucla.mbi.dxf14.DxfJAXBContext;
import edu.ucla.mbi.services.Fault;
import edu.ucla.mbi.services.ServiceException;
import edu.ucla.mbi.cache.NativeRecord;

import java.util.Map;

public abstract class RemoteNativeServer
    implements RemoteServer {

    private static Map<String,Object> context;
    String restUrl;
    String restAcTag;

    public boolean isNative(){
        return true;
    }
    
    public String getAddress(){
        return null;
    };
    
    public void setContext( Map<String,Object> context ) {
        RemoteNativeServer.context = context;
    }

    public Map<String,Object> getContext() {
	    return context;
    }

    public void setRestUrl( String url ) {
	    url = url.replaceAll("^\\s+","");
	    url = url.replaceAll("\\s+$","");
	
	    this.restUrl=url;
    }
    
    public String getRestUrl(){
	    return restUrl;
    }

    public void setRestAcTag( String tag ) {
	    tag = tag.replaceAll("^\\s+","");
	    tag = tag.replaceAll("\\s+$","");
        
	    this.restAcTag = tag ;
    }

    public String getRestAcTag(){
	    return restAcTag;
    }

    public void initialize() {
	    Log log = LogFactory.getLog( RemoteServer.class );
	    log.info("Initializing: " + this );
    }

    // RemoteServer 
    //-------------

    abstract public NativeRecord getNative( String provider, String service,
                                            String ns, String ac, int timeout 
                                            ) throws ServiceException; 
    
    
    public DatasetType transform( String strNative,
				                  String ns, String ac, String detail,
				                  String service, ProxyTransformer pTrans 
                                  ) throws ServiceException {

	    Log log = LogFactory.getLog( RemoteServer.class );
	    
        try {
	        // native data in string representationa as input
	    
	        ByteArrayInputStream bisNative =
		    new ByteArrayInputStream( strNative.getBytes( "UTF-8" ) );
	        StreamSource ssNative = new StreamSource( bisNative );
	    
	        // dxf as JAXBResult result of the transformation
	    
	        JAXBContext dxfJc = DxfJAXBContext.getDxfContext();
	        //JAXBContext.newInstance( "edu.ucla.mbi.dxf14" );
	        JAXBResult result = new JAXBResult( dxfJc );
	    
	        //transform into DXF
	    
	        pTrans.setTransformer( service );
    	    pTrans.setParameters( detail, ns, ac );
    	    pTrans.transform( ssNative, result );
	    
	        DatasetType dxfResult  = 
		    (DatasetType) ( (JAXBElement) result.getResult() ).getValue();
	    
            //test if dxfResult is empty
	        if ( dxfResult.getNode().isEmpty() ) {
		        throw Fault.getServiceException( 5 );  // no hits
	        }	    
            return dxfResult;
	    
	    } catch ( ServiceException fault ) { 
	        log.info( "Transformer fault: empty dxfResult ");
	        throw fault;
        } catch ( Exception e ) {
	        throw Fault.getServiceException( 99 );  // no hits
	    
	    }   
    }
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
				                 String detail, String service, 
				                 ProxyTransformer pTrans  
	                             ) throws ServiceException {
	
    	// NOTE: overload if dxf building more complex than
	    //       a simple xslt transformation
	
	    return this.transform( strNative, ns, ac, detail, service, pTrans );
    }
}

