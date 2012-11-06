package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL:: https://imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/src/main/ja#$
 * $Id:: RemoteNativeServer.java 2679 2012-08-30 01:49:50Z wyu                 $
 * Version: $Rev:: 2679                                                        $
 *==============================================================================
 *
 * RemoteServerImpl:
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
import edu.ucla.mbi.server.WSContext;

import java.util.*;

public class RemoteServerImpl implements RemoteServer {

    private Log log = LogFactory.getLog( RemoteServerImpl.class );
    protected Map<String,Object> context = new HashMap();
    protected NativeRestServer nativeRestServer = null;

    public boolean isNative() {
        return true;
    }
    
    public String getAddress() {
        return null;
    }
    
    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }

    public void setNativeRestServer ( NativeRestServer server ) {
        this.nativeRestServer = server;
    }

    public NativeRestServer getNativeRestServer() {
        return nativeRestServer;
    }

    public Map<String,Object> getContext() {
	    return context;
    }

    public void initialize() {
	    log.info("Initializing: " + this );
    }

    // Remore Native Server 
    //---------------------

    public NativeRecord getNative( String provider, String service,
                                   String ns, String ac, int timeout, 
                                   int retry ) throws ProxyFault {

        
        if( nativeRestServer == null ) {
            log.warn ( "getNative:restServer is not configured " + 
                       "for the service=" + service );

            throw FaultFactory.newInstance( Fault.UNSUPPORTED_OP );      
        } 
        
        return nativeRestServer.getNative( provider, service, ns, ac, timeout );
    }
    
    
    public DatasetType transform( String strNative,
				                  String ns, String ac, String detail,
				                  String provider, String service 
                                  ) throws ProxyFault {

	    Log log = LogFactory.getLog( RemoteServer.class );
	    
        try {
	        //*** native data in string representationa as input
	    
	        ByteArrayInputStream bisNative =
		    new ByteArrayInputStream( strNative.getBytes( "UTF-8" ) );
	        StreamSource ssNative = new StreamSource( bisNative );
	    
	        //*** dxf as JAXBResult result of the transformation
	    
	        JAXBContext dxfJc = DxfJAXBContext.getDxfContext();
	        //JAXBContext.newInstance( "edu.ucla.mbi.dxf14" );
	        JAXBResult result = new JAXBResult( dxfJc );
	    
	        //*** transform into DXF
	        ProxyTransformer pTrans = WSContext.getTransformer();

	        //pTrans.setTransformer( service );
            pTrans.setTransformer( provider, service );
    	    pTrans.setParameters( detail, ns, ac );
    	    pTrans.transform( ssNative, result );
	    
	        DatasetType dxfResult  = 
		    (DatasetType) ( (JAXBElement) result.getResult() ).getValue();
	    
            //*** test if dxfResult is empty
	        if ( dxfResult.getNode().isEmpty() ) {
		        throw FaultFactory.newInstance( Fault.NO_RECORD );  // no hits
	        }	    
            return dxfResult;
	    
	    } catch ( ProxyFault fault ) { 
	        log.info( "Transformer fault: empty dxfResult ");
	        throw fault;
        } catch ( Exception e ) {
	        throw FaultFactory.newInstance( Fault.UNKNOWN );  
	    }   
    }
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
				                 String detail, String provider, 
                                 String service ) throws ProxyFault 
    {
	
    	// NOTE: overload if dxf building more complex than
	    //       a simple xslt transformation
	
	    return this.transform( strNative, ns, ac, detail, provider, service );
    }
}

