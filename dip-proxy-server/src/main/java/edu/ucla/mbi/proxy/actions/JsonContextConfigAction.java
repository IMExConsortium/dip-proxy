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

        for( Iterator<String> i = getOp().keySet().iterator();
             i.hasNext(); ) {
            
            String key = i.next();
            String val = getOp().get(key);
            
            log.debug(  "op=" + key + "  val=" + val );

            if ( val != null && val.length() > 0 ) {
                 
                if( key.equals( "add" ) && val.equals( "map" ) ) {
                    log.info( "execute: op.add map hit. " );
                    return operationAction ( key + val );
                }

                if( key.equals( "set" ) && val.equals( "prop" ) ) {
                    log.info( "execute: op.set prop hit. " );
                    return operationAction ( key + val );
                }

                if( key.equals( "drop" )  && val.equals( "prop" ) ) {
                    return operationAction ( key + val );
                }

                if( key.equals( "drop" ) && val.equals( "map" ) ) { 
                    log.info( "execute: op.drop map hit. " );
                    return operationAction ( key + val );
                }

                if( key.equals( "show" ) ) {
                    log.info( "execute: op.show hit. " );
                    return "json";
                }

                if( key.equals( "clear" ) ) {
                    log.info( "execute: op.clear hit." );
                    if( getOpp() != null ) {
                        clearJsonWithOpp();
                    }
                    return SUCCESS;
                }

            } else {
                return SUCCESS;
            }        
        }

        log.info( "execute: return fault.");

        return ERROR;
    }
  
    private String operationAction ( String op ) throws ProxyFault {
        
        SortedSet<String> levelSSet = new TreeSet();
        String propKey = null;
        String propValue = null;

        for( String oppKey:getOpp().keySet() ) {
            if( oppKey.startsWith( "l" ) ) {
                Integer level = Integer.valueOf(
                                    oppKey.substring(1) );

                if( level.intValue() <= contextDepth ) {
                    levelSSet.add( oppKey );
                } else {
                    log.warn( "op.drop prop: level > contextDepth. " );
                    return ERROR;      
                }
            } 
                
            if ( oppKey.equals( "key" ) ) {
                propKey = getOpp().get(oppKey);
            } 

            if ( oppKey.equals( "value" ) ) {
                propValue = getOpp().get(oppKey);
            }

        }
        
        String[] levelArray = levelSSet.toArray( new String[0] );
        boolean isNew = false; // for new add map
        Map<String, Object> currentMap = new HashMap();
        Map<String, Object> parentMap = new HashMap();

        currentMap = (Map<String, Object>) contextMap.get(contextTop);

        for( int i = 0; i < levelArray.length; i++ ) {
            log.info( "operationAction: level=" + levelArray[i] ) ;
            log.info( "operationAction: level value=" + getOpp().get(levelArray[i]) );
            log.info( "operationAction: currentMap=" + currentMap );

            parentMap = currentMap;

            if( isNew || currentMap.get( getOpp().get(levelArray[i])) == null ) {
                log.info( "operationAction: isNew=" + isNew );
                Map<String, Object> levelMap = new HashMap();
                currentMap.put( getOpp().get(levelArray[i]), levelMap );
                isNew = true;
            }

            currentMap = (Map<String, Object>)currentMap.get(
                                            getOpp().get(levelArray[i]));
           
            log.info( "operationAction: after get i: currentMap=" + currentMap ); 

            if( i == levelArray.length - 1 ) {
                if( op.equals( "addmap" ) && isNew ) {
                    log.info( "operationAction: add map... " );
                    saveJsonContext();
                    return SUCCESS;
                }

                if( op.equals( "setprop" ) && (
                        currentMap.get(propKey) == null  
                        ||  !currentMap.get(propKey).equals( propValue ) ) )
                { 
                    log.info( "operationAction: set prop.." );
                    currentMap.put( propKey, propValue ); //add or update property
                    saveJsonContext();
                    return SUCCESS;
                }

                if( op.equals( "dropmap" ) && !isNew && currentMap != null ) {
                    log.info( "operationAction: drop map... ");
                    parentMap.remove( getOpp().get(levelArray[i]) );    
                    saveJsonContext();
                    return SUCCESS;      
                }

                if( op.equals( "dropprop" ) && !isNew 
                    && currentMap.get(propKey) != null ) 
                {
                    log.info( "operationAction: drop prop: key=" + propKey );
                    currentMap.remove(propKey); //remove property
                    saveJsonContext();
                    return SUCCESS;
                } 
                
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
    
