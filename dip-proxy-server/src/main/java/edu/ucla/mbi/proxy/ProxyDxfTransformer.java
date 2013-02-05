package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL:: https://imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-proxy-s#$
 * $Id:: ProxyTransformer.java 2960 2013-02-05 02:14:45Z wyu                   $
 * Version: $Rev:: 2960                                                        $
 *==============================================================================
 *
 * ProxyDxfTransformer:
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayInputStream;

import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBContext;

import javax.xml.transform.stream.StreamSource;

import edu.ucla.mbi.dxf14.DatasetType;
import edu.ucla.mbi.dxf14.DxfJAXBContext;

import edu.ucla.mbi.server.WSContext;

import edu.ucla.mbi.util.context.*;
import edu.ucla.mbi.fault.*;

public class ProxyDxfTransformer implements ContextListener {

    public ProxyDxfTransformer(){}
    
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
	        JAXBResult result = new JAXBResult( dxfJc );
                
	        //*** transform into DXF
	        ProxyTransformer pTrans = WSContext.getTransformer();
                
                //synchronize{

                pTrans.setTransformer( provider, service );
                pTrans.setParameters( detail, ns, ac );
                pTrans.transform( ssNative, result );
                
                //}
                
                DatasetType dxfResult  = 
                    (DatasetType) ( (JAXBElement) result.getResult() ).getValue();
                
                //*** test if dxfResult is empty
                if ( dxfResult.getNode().isEmpty() 
                     || dxfResult.getNode().get(0).getAc().equals("") ) {
                    
                    throw FaultFactory.newInstance( Fault.TRANSFORM );  
	        }	    
                return dxfResult;
                
	    } catch ( ProxyFault fault ) { 
	        log.info( "Transformer fault: empty dxfResult ");
	        throw fault;
            } catch ( Exception e ) {
	        throw FaultFactory.newInstance( Fault.TRANSFORM );  
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
