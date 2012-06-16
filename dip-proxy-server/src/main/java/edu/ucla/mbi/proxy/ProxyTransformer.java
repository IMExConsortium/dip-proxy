package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * ProxyTransformer:
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.bind.util.JAXBResult;

import org.springframework.core.io.*;

public class ProxyTransformer{
    private Transformer tf;
    private Map<String, Resource> transfMap;
    private Map<String, Object> context;
    
    public void setTransfMap(Map<String,Resource> transfMap){
	    this.transfMap = transfMap;
    }

    public void setContext(Map<String,Object> context){
	    this.context=context;
    }

    public Transformer getTransformer() {
	    return tf;
    }
    
    public void setTransformer( String service ) { 
        Log log = LogFactory.getLog( ProxyTransformer.class );
        try {
	    
            DocumentBuilderFactory 
	    	dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware( true );
	    
            DocumentBuilder db = dbf.newDocumentBuilder();
	    
	        // NOTE: this one needed when file sits inside .aar
	        //ClassLoader cl = getClass().getClassLoader();
	        //Document xslDoc = db.parse(cl.getResourceAsStream(xslFile));	   
	    
            Resource xslFile =transfMap.get( service );
            Document xslDoc = db.parse( xslFile.getFile() );
	    
            DOMSource xslDomSource = new DOMSource( xslDoc );
            TransformerFactory 
                tFactory = TransformerFactory.newInstance();
	    
            ErrorListener logErrorListener = new TransformLogErrorListener();	    
            tFactory.setErrorListener( logErrorListener );
	    
            this.tf = tFactory.newTransformer( xslDomSource );                                  
            tf.setErrorListener( logErrorListener );
	    
        } catch( Exception e ) {
            log.info( e.toString() );
            this.tf = null;
        }     
    }   
    
    public void setParameters( String detail, String ns, String ac) {
	
	    Log log = LogFactory.getLog( ProxyTransformer.class );
	
	    tf.clearParameters();
	    tf.setParameter("edu.ucla.mbi.services.detail", detail);
	    tf.setParameter("edu.ucla.mbi.services.ns", ns);
	    tf.setParameter("edu.ucla.mbi.services.ac", ac);
    }  
 
    public void transform( StreamSource xmlStreamSource, 
                           JAXBResult jaxbResult) {
	    
	    Log log = LogFactory.getLog( ProxyTransformer.class );
	
	    try{
	        tf.transform(xmlStreamSource, jaxbResult );
	    }catch(Exception e){
	        log.info("Transformation error="+e.toString());
	        // NOTE: should throw exception/fault
	    }
    }
}
