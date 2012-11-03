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

import edu.ucla.mbi.util.*;
import edu.ucla.mbi.fault.*;

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.bind.util.JAXBResult;
import javax.servlet.ServletContext;

import org.springframework.core.io.*;
import org.springframework.web.context.ServletContextAware;

public class ProxyTransformer implements ServletContextAware {

    private JsonContext transformerContext;
    private ServletContext servletContext;

    private Transformer tf;
    //private Map<String, Resource> transfMap;
    private Map<String, Object> transfMap;

    //private Map<String, Object> context;

    //*** setter
    public void setTransformerContext ( JsonContext context ) {
        this.transformerContext = context;
    }    

    public void setServletContext ( ServletContext servletContext ) {
        this.servletContext = servletContext;
    }

    /*
    public void setTransfMap(Map<String,Resource> transfMap){
	    this.transfMap = transfMap;
    }

    public void setContext(Map<String,Object> context){
	    this.context=context;
    } */

    //*** getter
    public JsonContext getTransformerContext () {
        return transformerContext;
    }

    public Transformer getTransformer() {
	    return tf;
    }
   
   /* 
    public Map<String, Resource> getTransfMap() {
        return transfMap;
    }*/  

    public Map<String, Object> getTransfMap() {
        return transfMap;
    }

    public void initialize () throws ProxyFault {
        Log log = LogFactory.getLog( ProxyTransformer.class );
        log.info( "initializing ... " );
    
        String jsonConfigFile =
                (String) transformerContext.getConfig().get( "json-config" );

        String srcPath = servletContext.getRealPath( jsonConfigFile );

        try {
            transformerContext.readJsonConfigDef( srcPath );
        } catch( Exception e ) {
            log.info( "configInitialize exception: " + e.toString() );
            throw FaultFactory.newInstance ( 27 ); // json configuration
        }

        Map<String, Object> jtf = transformerContext.getJsonConfig();

        transfMap = (Map) jtf.get( "transformer" );

    }

    //public void setTransformer( String service ) { 
    public void setTransformer( String provider, 
                                String service, 
                                String tfType ) {

        Log log = LogFactory.getLog( ProxyTransformer.class );
        try {
	    
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware( true );
	    
            DocumentBuilder db = dbf.newDocumentBuilder();
	    
	        // NOTE: this one needed when file sits inside .aar
	        //ClassLoader cl = getClass().getClassLoader();
	        //Document xslDoc = db.parse(cl.getResourceAsStream(xslFile));	   
	    
            /* NOTE: this one needed in spring configureation 
            Resource xslFile =transfMap.get( service );
            Document xslDoc = db.parse( xslFile.getFile() );
            */

            String xslFilePath = (String)((Map)((Map) transfMap.get(provider))
                                                    .get(service)).get(tfType);

            String xslRealPath = servletContext.getRealPath( xslFilePath );

            log.info( "setTransformer: xslRealPath=" + xslRealPath );

            Document xslDoc = db.parse ( new File( xslRealPath ) );

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
