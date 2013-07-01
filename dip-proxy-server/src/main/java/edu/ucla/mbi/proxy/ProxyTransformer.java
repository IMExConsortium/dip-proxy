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
import edu.ucla.mbi.dxf14.*;

import java.util.*;
import java.io.*;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.StreamSource;
import javax.xml.bind.util.JAXBResult;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBContext;

public class ProxyTransformer implements ContextListener {

    private JsonContext transformerContext;
    private String contextTop;
    
    private Transformer tf;
    private static Map<String, Object> transfMap;

    //*** setter
    public void setTransformerContext ( JsonContext context ) {
        this.transformerContext = context;
    }    

    public void setContextTop ( String top ) {
        this.contextTop = top;
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

    public void initialize () throws ServerFault {

        Log log = LogFactory.getLog( ProxyTransformer.class );
        log.info( "initializing ... " );
    
        FileResource fr = (FileResource) transformerContext
                                .getConfig().get("json-source");

        if ( fr == null ) return;

        try {
            transformerContext.readJsonConfigDef( fr.getInputStream() );
        } catch ( Exception e ){
            log.info( "initialize exception: " + e.toString() );
            throw ServerFaultFactory.newInstance ( Fault.JSON_CONFIGURATION ); 
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
        throws ServerFault {

        Log log = LogFactory.getLog( ProxyTransformer.class );
        
        try {
	    
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware( true );
	    
            DocumentBuilder db = dbf.newDocumentBuilder();
	    
            String tfType = (String)((Map)((Map) transfMap.get(provider))
                                                    .get(service)).get("type");

            if( tfType != null && tfType.equals( "xslt" ) ) {
                
                String xslFilePath = (String)((Map)((Map) transfMap
                    .get(provider)).get(service)).get("xslt");

                SpringFileResource fr = (SpringFileResource)transformerContext
                                            .getConfig().get("json-source");

                if ( fr == null ) throw FaultFactory.newInstance( 
                                            Fault.JSON_CONFIGURATION ); 

                fr.setFile( xslFilePath  );
                
                Document xslDoc = db.parse ( fr.getInputStream() );

                DOMSource xslDomSource = new DOMSource( xslDoc );

                TransformerFactory 
                    tFactory = TransformerFactory.newInstance();
	    
                ErrorListener logErrorListener = new TransformLogErrorListener();	    
                tFactory.setErrorListener( logErrorListener );
	    
                this.tf = tFactory.newTransformer( xslDomSource );                                  
                tf.setErrorListener( logErrorListener );
            } else {
                log.info( "setTransformer: tfType == null or tfType != 'xslt' " );
                throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
            }
        } catch ( ServerFault fault ) {
            log.info( "setTransformer: transformer.json doesn't have " + 
                      "transformer type xslt. " );
            throw fault;
        } catch( Exception e ) {
            log.info( "setTransformer: exception is " + e.toString() );
            throw  ServerFaultFactory.newInstance( Fault.UNKNOWN ); 
        }     
    }   
    
    public void setParameters( String detail, String ns, String ac) {
	
	    Log log = LogFactory.getLog( ProxyTransformer.class );
	
	    tf.clearParameters();
	    tf.setParameter("edu.ucla.mbi.services.detail", detail);
	    tf.setParameter("edu.ucla.mbi.services.ns", ns);
	    tf.setParameter("edu.ucla.mbi.services.ac", ac);
    }  

    public DatasetType transform( String strNative, String detail ) 
        throws ServerFault {
        
        Log log = LogFactory.getLog( ProxyTransformer.class );

        try{
            //*** native data in string representationa as input

            ByteArrayInputStream bisNative =
            new ByteArrayInputStream( strNative.getBytes( "UTF-8" ) );
            StreamSource ssNative = new StreamSource( bisNative );

            //*** dxf as JAXBResult result of the transformation
            JAXBContext dxfJc = DxfJAXBContext.getDxfContext();
            JAXBResult result = new JAXBResult( dxfJc );

            tf.transform( ssNative, result );

            DatasetType dxfResult  =
                (DatasetType) ( (JAXBElement) result.getResult() ).getValue();

            //*** test if dxfResult is empty
            if ( dxfResult.getNode().isEmpty()
                 || dxfResult.getNode().get(0).getAc().equals("") ) {

                throw ServerFaultFactory.newInstance( Fault.TRANSFORM );
            }

            return dxfResult;

        } catch ( ServerFault fault ) {
            log.info( "Transformer fault: empty dxfResult ");
            throw fault;
        }catch(Exception e){
            log.info("Transformation error=" + e.toString());
            throw ServerFaultFactory.newInstance ( Fault.TRANSFORM ); // transformer error
        }
    }
}
