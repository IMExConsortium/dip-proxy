package edu.ucla.mbi.proxy.actions;

/*===========================================================================
 * $HeadURL:: https://imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-prox#$
 * $Id:: NativeServerConfigure.java 2812 2012-11-07 23:14:55Z lukasz        $
 * Version: $Rev:: 2812                                                     $
 *===========================================================================
 *
 * NativeServerConfigure Action:
 *
 *========================================================================= */

import com.opensymphony.xwork2.ActionSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.fault.*;
import java.util.*;
import java.io.*;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.util.JsonContext;
import edu.ucla.mbi.util.struts2.action.ManagerSupport;

public class JsonContextConfigAction extends ManagerSupport {

    private Log log = LogFactory.getLog( JsonContextConfigAction.class );

    private final String JSON = "json"; 
    private Map<String, Object> topMap;
     
    private JsonContext jsonContext;
    
    private Map<String, Object> contextMap; 
    private String contextTop; 
    private int contextDepth = 1;

    //*** setter
    public void setJsonContext( JsonContext context ) {
        this.jsonContext = context;
    }

    public void setContextTop( String top ) {
        contextTop = top;
    }

    public void setContextDepth( int depth ) {
        contextDepth = depth;
    }
 
    //*** getter    
    public Map<String, Object> getContextMap() {
        return contextMap;
    } 

    public String getContextTop() {
        return contextTop;
    }

    public int getContextDepth() {
        return contextDepth;
    }

    //---------------------------------------------------------------------

    public String execute() throws Exception {

        log.info( " JsonContextConfigAction execute..." );
        
        //*** read json file
        String jsonConfigFile =
            (String) jsonContext.getConfig().get( "json-config" );
        String srcPath = getServletContext().getRealPath( jsonConfigFile );

        try {
            jsonContext.readJsonConfigDef( srcPath );
        } catch( Exception e ) {
            log.info( "configInitialize exception: " + e.toString() );
            throw FaultFactory.newInstance ( 27 ); // json configuration
        }
        
        contextMap = jsonContext.getJsonConfig();
        topMap = (Map<String, Object>)contextMap.get( contextTop );

        log.info( "execute: contextMap=" + contextMap );
        log.info( "contextDepth=" + contextDepth );
        
        if( getOp() == null ) {
            log.info( "execute: enter op=view.");
            return SUCCESS;  //XXX
        } 

        for( String opKey:getOp().keySet() ) {
            
            String opVal = getOp().get(opKey);
            
            log.info(  "op=" + opKey + "  and val=" + opVal );

            if ( opVal != null && opVal.length() > 0 ) {
                return operationAction ( opKey, opVal );
            } 
        }

        log.info( "execute: return fault.");

        return ERROR;
    }
  
    private String operationAction ( String opKey, String opValue ) throws ProxyFault {
        
        if( opKey.equals( "show" ) ) {
            log.info( "execute: op.show hit. " );
            return "json";
        }

        if( opKey.equals( "clear" ) ) {
            log.info( "execute: op.clear hit." );
            if( getOpp() != null ) {
                clearJsonWithOpp();
            }
            return SUCCESS;
        }

        String propKey = null;
        String propValue = null;
        String[] levelArray = new String [ contextDepth ];
        int maxOfLevel = 0; // this value <= contextDepth

        for( String oppKey:getOpp().keySet() ) {
            String oppVal = getOpp().get( oppKey );

            if( oppKey.startsWith( "l" ) ) {
                int level = Integer.valueOf( oppKey.substring(1) ).intValue();
                if( level <= contextDepth ) {
                    levelArray[ level -1 ] =  oppVal;
                    if( level > maxOfLevel ) {
                        maxOfLevel = level;
                    }
                } else {
                    log.warn( "op: " + opKey + "/" + opValue 
                              + ": level > contextDepth. " );
                    return ERROR; 
                }
            } 
                
            if ( oppKey.equals( "key" ) ) {
                propKey = oppVal;
            } 

            if ( oppKey.equals( "value" ) ) {
                propValue = oppVal;
            }
        }
        
        //*** validate levelArray
        for( int i = 0; i < maxOfLevel; i++ ) { 
            if( levelArray[i] == null ) {
                log.warn( "opp level is not consistent. " );
                return ERROR;
            }
        } 

        boolean isNewMap = false; // check if the level is new added
        Map<String, Object> currentMap = new HashMap();
        Map<String, Object> parentMap = new HashMap();

        currentMap = (Map<String, Object>) contextMap.get(contextTop);

        for( int i = 0; i < maxOfLevel; i++ ) {

            String levelKey = levelArray[i];

            parentMap = currentMap;

            if( currentMap.get( levelKey ) == null ) {
                if(  i == maxOfLevel - 1 ) {
                    Map<String, Object> levelMap = new HashMap();
                    currentMap.put( levelKey, levelMap );
                    isNewMap = true;
                } else {
                    log.warn( "operationAction: level(l" + i + "=" 
                              + levelArray[i] + ")  does not exist. " );
                    return ERROR;
                }
            }

            currentMap = (Map<String, Object>)currentMap.get( levelArray[i] );
        }           
            
        log.info( "operationAction: after get i: currentMap=" + currentMap ); 

        if( isNewMap ) {
            if( opKey.equals("add") && opValue.equals("map") ) {
                log.info( "operationAction: add map... " ); // add a map
                saveJsonContext();
                return SUCCESS;
            }
        } else {
            if( opKey.equals("set") && opValue.equals("prop") 
                && ( currentMap.get(propKey) == null  
                        ||  !currentMap.get(propKey).equals( propValue ) ) )
            { 
                log.info( "operationAction: set prop.." );
                currentMap.put( propKey, propValue ); //add or update property
                saveJsonContext();
                return SUCCESS;
            }

            if( opKey.equals("drop") && opValue.equals("map") 
                && currentMap != null ) 
            {
                log.info( "operationAction: drop map... ");
                parentMap.remove( levelArray[ maxOfLevel - 1 ]); // drop a map   
                saveJsonContext();
                return SUCCESS;      
            }

            if( opKey.equals("drop") && opValue.equals("prop") 
                && currentMap.get(propKey) != null ) 
            {
                log.info( "operationAction: drop prop: key=" + propKey );
                currentMap.remove(propKey); //remove property
                saveJsonContext();
                return SUCCESS;
            } 
        }

        return ERROR;
    }

    private void clearJsonWithOpp () throws ProxyFault {
 
        for( String oppKey:getOpp().keySet() ) {
            
            if( !oppKey.contains( "_" ) ) {
                continue;
            } 

            String[] oppKeyArray = oppKey.split( "_" );
            String provider = oppKeyArray[0];
            String service = oppKeyArray[1];
            String serverKey = oppKeyArray[2];
            
            Map<String, Object> jsonProviderMap = 
                (Map<String, Object>) ( (Map<String, Object>) contextMap
                                        .get( contextTop ) ).get( provider ) ;
            
            if( jsonProviderMap == null ) {
                //*** create new provider in Json object        
                jsonProviderMap = new HashMap();
            }

            Map<String, Object> jsonServiceMap =
                (Map<String, Object>) jsonProviderMap.get( service );
            
            if( jsonServiceMap == null ) {
                //*** create new service in Json object
                jsonServiceMap = new HashMap();
            }
            
            String jsonServerValue = 
                (String)jsonServiceMap.get( serverKey );
            
            jsonServerValue = ""  ;
            
            jsonServiceMap.put( serverKey, jsonServerValue );
            jsonProviderMap.put( service, jsonServiceMap );
            
            ((Map<String, Object>) contextMap.get( contextTop) )
                .put( provider, jsonProviderMap);
            
        }        
        
    }
    
    private void saveJsonContext() throws ProxyFault {
            
        String jsonConfigFile = (String) jsonContext.getConfig()
            .get( "json-config" );
        
        log.info( "saveJsonContext: jsonConfigFile=" + jsonConfigFile );

        String srcPath =
            getServletContext().getRealPath( jsonConfigFile );
        
        log.info( "saveJsonContext:  srcPath=" + srcPath );
        
        try { 
            jsonContext.writeJsonConfigDef( srcPath  );
            
        } catch ( Exception e ) {
            log.info( " saveNativeServerConfigure exception: " + e.toString() );
            throw FaultFactory.newInstance ( 27 ); // json configuration         
        }

        log.info( "saveNativeServerConfigure: after writing to json file. " ); 
    }
        
}
    
