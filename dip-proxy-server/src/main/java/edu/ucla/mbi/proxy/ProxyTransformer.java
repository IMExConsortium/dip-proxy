package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProxyTransformer:
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 
import edu.ucla.mbi.util.context.*;
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

//import org.springframework.core.io.*;
//import org.springframework.web.context.ServletContextAware;

public class ProxyTransformer implements //ServletContextAware, 
                                         ContextListener {

    private JsonContext transformerContext;
    private String contextTop;
    private ServletContext servletContext;
    
    private Transformer tf;
    private static Map<String, Object> transfMap;

    //*** setter
    public void setTransformerContext ( JsonContext context ) {
        this.transformerContext = context;
    }    

    public void setContextTop ( String top ) {
        this.contextTop = top;
    }

    public void setServletContext ( ServletContext servletContext ) {
        this.servletContext = servletContext;
    }

    //*** getter
    public JsonContext getTransformerContext () {
        return transformerContext;
    }

    public String getContextTop () {
        return contextTop;
    }

    public Transformer getTransformer() {
	    return tf;
    }
   
    public static Map<String, Object> getTransfMap() {
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

        transfMap = (Map) jtf.get( contextTop );
    
        //*** add context listener
        transformerContext.addContextUpdateListener( this );
        log.info ( "initialize.. after get transformer map. " );    
    }

    public void contextUpdate ( JsonContext context ) {

        Log log = LogFactory.getLog( ProxyTransformer.class );
        log.info( "contextUpdate called. " );
       
        Map<String, Object> jtf = context.getJsonConfig();

        transfMap = (Map) jtf.get( contextTop );
    }

    public void setTransformer( String provider, String service ) 
        throws ProxyFault {

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

            String tfType = (String)((Map)((Map) transfMap.get(provider))
                                                    .get(service)).get("type");

            if( tfType != null && tfType.equals( "xslt" ) ) {
                String xslFilePath = (String)((Map)((Map) transfMap
                    .get(provider)).get(service)).get("xslt");

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
            } else {
                log.info( "setTransformer: tfType == null or tfType != 'xslt' " );
                throw FaultFactory.newInstance( 27 ); //json configuration
            }
        } catch ( ProxyFault fault ) {
            log.info( "setTransformer: transformer.json doesn't have " + 
                      "transformer type xslt. " );
            throw fault;
        } catch( Exception e ) {
            log.info( "setTransformer: exception is " + e.toString() );
            throw  FaultFactory.newInstance( 99 ); //unknown;
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
                           JAXBResult jaxbResult) throws ProxyFault {
	    
	    Log log = LogFactory.getLog( ProxyTransformer.class );

	    try{
	        tf.transform(xmlStreamSource, jaxbResult );
	    }catch(Exception e){
	        log.info("Transformation error=" + e.toString());
            throw FaultFactory.newInstance ( 7 ); // transformer error
	    }
    }
}
