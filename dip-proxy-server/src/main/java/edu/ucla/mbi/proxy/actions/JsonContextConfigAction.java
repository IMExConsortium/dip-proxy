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
    private final String MAP = "map";
    private final String LIST = "list";

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
            return SUCCESS; 
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
  
    private String operationAction ( String opKey, String opVal ) throws ProxyFault {
        
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

        String setKey = null;
        String setVal = null;
        String[][] levelArray = new String [ contextDepth ][2];
        int maxOfLevel = 0; // this value <= contextDepth

        //*** fill levelArray using oppVal
        for( String oppKey:getOpp().keySet() ) {
            String oppVal = getOpp().get( oppKey );

            if( oppKey.startsWith( "m" ) || oppKey.startsWith( "l" ) ) {
                //*** m means map and l means list
                int level;

                try {
                    level = Integer.valueOf( oppKey.substring(1) ).intValue();
                } catch ( Exception e ) {
                    log.warn( "op: oppKey(" + oppKey + ") format wrong. " );
                    return ERROR;
                }

                if( level > 0 && level <= contextDepth ) {
                    if( oppKey.startsWith( "m" ) ) {
                        levelArray[ level -1 ][0] =  oppVal;
                    } 

                    if( oppKey.startsWith( "l" ) ) {
                        if( oppVal.matches( "([0]|[1-9][0-9]*)" ) ) {
                            //*** oppVal is a number that is an index in the list 
                            levelArray[ level -1 ][1] =  oppVal;
                        } else {
                            log.warn( "opAction: opp(" + oppKey + "=" + 
                                      oppVal + ") oppVal has to be a number. " );
                            return ERROR;
                        }
                    } 

                    if( level > maxOfLevel ) {
                        maxOfLevel = level;
                    }
                } else {
                    log.warn( "opAction: opp(" + opKey + "=" + opVal + ") " +
                              "level should not be greater than contextDepth. " );
                    return ERROR; 
                }
            } 
                 
            if ( oppKey.equals( "key" ) ) {
                setKey = oppVal;
            } 

            if ( oppKey.equals( "value" ) ) {
                setVal = oppVal;
            }
        }
        
        //*** validate levelArray
        for( int i = 0; i < maxOfLevel; i++ ) { 
            if( ( levelArray[i][0] == null && levelArray[i][1] == null )
                  || ( levelArray[i][0] != null && levelArray[i][1] != null ) ) 
            {
                log.warn( "opp level is not consistent. " );
                return ERROR;
            }
            log.info( "opAction: levelArray 0=" + levelArray[i][0] +
                  " levelArray 1=" + levelArray[i][1] + "." );
        } 

        boolean isNew = false; // check if the level is new added
        Object currentObj = null;
        Object parentObj = null;
        String currentLevelType = null;  
        String parentLevelType = null;  

        if( contextMap.get(contextTop) instanceof Map ) {
            currentLevelType = MAP;
            currentObj = (Map<String, Object>) contextMap.get(contextTop);
        } else if( contextMap.get(contextTop) instanceof List ) {
            currentLevelType = LIST;
            currentObj = (List) contextMap.get(contextTop);
        } else {
            log.warn( "opAction: context top type is neither Map nor List. " );
            return ERROR;
        }

        //*** trace level tree
        for( int i = 0; i < maxOfLevel; i++ ) {
            
            String levelKey = null;

            parentObj = currentObj;
            parentLevelType = currentLevelType;

            if( levelArray[i][0] != null ) {
                
                if( !parentLevelType.equals( MAP ) ) {
                    log.warn( "opAction: opp level(" + i + ") type does not " +
                              "match with json file. " );
                    return ERROR;
                }
               
                levelKey = levelArray[i][0];

                if( ((Map<String, Object>)parentObj)
                            .get( levelKey ) == null ) 
                { 
                    if(  i == maxOfLevel - 1 && opKey.equals("add") ) {
                        if( opVal.equals( MAP ) ) {
                            Map<String, Object> levelMap = new HashMap();
                            ((Map<String, Object>)parentObj)
                                .put( levelKey, levelMap );

                            isNew = true;
                        } else if ( opVal.equals( LIST ) ) {
                            List<Object> levelList = new ArrayList();
                            ((Map<String, Object>)parentObj)
                                .put( levelKey, levelList );

                            isNew = true;
                        } else {
                            log.warn( "opAction: operation add for " +
                                      "value(" + opVal + ") neither " + 
                                      "map nor list. " );
                            return ERROR;
                        }
                    } else {
                        log.warn( "opAction: opp level(" + i + ") does not " +
                                  "match with json file. " );
                        return ERROR;
                    }
                } 

                currentObj = ((Map<String, Object>)parentObj).get( levelKey );

            }

            if ( levelArray[i][1] != null ) {
                
                if( !parentLevelType.equals( LIST ) ) {
                    log.warn( "opAction: opp level(" + i + ") type does not " +
                              "match with json file. " );
                    return ERROR;
                }

                log.info( "opAction: before get index. " );    
                levelKey = levelArray[i][1];
                int index = Integer.valueOf(levelKey).intValue();
                log.info( "opAction: index=" + index );

                if( index == ((List)currentObj).size() ) {
                    if( i == maxOfLevel - 1 && opKey.equals("add") ) {
                        if( opVal.equals( MAP ) ) {
                            Map<String, Object> levelMap = new HashMap();
                            ((List)parentObj).add( levelMap );
                            isNew = true;
                        } else if( opVal.equals( LIST ) ) {
                            List levelList = new ArrayList();
                            ((List)parentObj).add( levelList );
                            isNew = true;
                        } else {
                            log.warn( "opAction: operation add for " +
                                      "value(" + opVal + ") neither " +
                                      "map nor list. " );
                            return ERROR;
                        }
                        currentObj = ((List)parentObj).get( index );
                    }
                } else if ( index < ((List)currentObj).size() ) { 
                    currentObj = ((List)parentObj).get( index );
                } else {
                    log.warn( "opAction: operation add for value(" +
                              opVal + ") has a wrong list index. " );  
                    return ERROR;
                } 
            }

            if( currentObj instanceof Map ) {
                currentLevelType = MAP;
            } else if ( currentObj instanceof List ) {
                currentLevelType = LIST;
            } else {
                log.warn( "opAction: context top type is neither Map nor List. " );
                return ERROR;
            }

        }           
            
        if( opKey.equals("add") && currentObj != null ) {
            if( isNew ) {
                log.info( "operationAction: add map... " ); // add a map/list
                saveJsonContext();
                return SUCCESS;
            }
        }
    
        if( opKey.equals("set") ) {
            if( opVal.equals( "prop" ) ) {
                if( currentLevelType.equals( MAP ) ) {
                    if( ((Map<String, Object>)currentObj).get(setKey) == null
                         || ( ((Map<String, Object>)currentObj)
                                    .get(setKey) instanceof String 
                                && !((Map<String, Object>)currentObj)
                                    .get(setKey).equals( setVal ) ) ) 
                    {
                        log.info( "operationAction: set prop.." );
                        ((Map<String, Object>)currentObj).put( setKey, setVal ); //add or update property
                        saveJsonContext();
                        return SUCCESS;
                    }
                } else {
                    log.warn( "opAction: op (" + opKey + "=" + opVal + ") failed," + 
                              " because the last level type is not Map. " );
                    return ERROR;
                }
            }

            if( opVal.equals( "ele" ) ) {
                if( currentLevelType.equals( LIST ) ) {
                    int index = Integer.valueOf( levelArray[maxOfLevel - 1][1] )
                                        .intValue();

                    boolean update = false;
                    
                    if( index == ((List)currentObj).size() ) {
                        ((List)currentObj).add( setVal );
                        update = true;
                    } else if( index < ((List)currentObj).size()  
                               && !((List)currentObj).get( index )
                                        .equals( setVal ) ) 
                    {

                        ((List)currentObj).set(index, setVal ); // add or update element
                        update = true;

                    } else {
                       log.warn( "opAction: op (" + opKey + "=" + opVal + 
                                 ") failed because the last level type " +
                                 "is not List or the index of List is wrong." );
                        return ERROR;
                    }
                        
                    if( update ) {
                        saveJsonContext();
                        return SUCCESS;
                    }
                }
            }   
        }

        if( opKey.equals("drop") && currentObj != null ) {
            if( opVal.equals("map") ) {
                if( currentLevelType.equals( MAP ) ) { 
                    log.info( "operationAction: drop map... ");
                    if( parentLevelType.equals( MAP ) ) {
                        ((Map<String, Object>)parentObj)
                            .remove( levelArray[ maxOfLevel - 1 ][0]); // drop a map   
                        saveJsonContext();
                        return SUCCESS;  
                    } 

                    if ( parentLevelType.equals( LIST ) ) {
                        ((List)parentObj).remove( levelArray[ maxOfLevel - 1 ][1] ); // drop a map
                        saveJsonContext();
                        return SUCCESS;
                    }     
                } else {
                    log.warn( "opAction: op (" + opKey + "=" + opVal + ") failed," + 
                              " because the current level type is not a Map. " );
                    return ERROR;
                }
            }
          
            if( opVal.equals("list") ) {
                if( currentLevelType.equals( LIST ) ) {    
                    log.info( "operationAction: drop list... ");

                    if( parentLevelType.equals( MAP ) ) {
                        ((Map<String, Object>)parentObj)
                            .remove( levelArray[ maxOfLevel - 1 ][0]); // drop a list   
                    }

                    if ( parentLevelType.equals( LIST ) ) {
                        ((List)parentObj).remove( 
                            levelArray[ maxOfLevel - 1 ][1] ); // drop a list
                    }

                    saveJsonContext();
                    return SUCCESS;
                } else {
                    log.warn( "opAction: op (" + opKey + "=" + opVal + ") failed," +
                              " because the current level type is not a List. " );
                    return ERROR;
                }
            }

            if( opVal.equals("prop") ) { 
                if( currentLevelType.equals( MAP ) ) {
                    if( ((Map<String, Object>)currentObj).get(setKey) != null  
                        && ((Map<String, Object>)currentObj)
                                .get(setKey) instanceof String ) 
                    {
                        log.info( "operationAction: drop prop: key=" + setKey );
                        ((Map<String, Object>)currentObj).remove(setKey); //remove property
                        saveJsonContext();
                        return SUCCESS;
                    } 
                } else {
                    log.warn( "opAction: op (" + opKey + "=" + opVal + ") failed," +
                              " because the current level type is not a Map. " );
                    return ERROR;
                }   
            }

            if( opVal.equals( "ele" ) ) {
                if( currentLevelType.equals( LIST ) ) {
                    log.info( "operationAction: drop a list element... ");
                     ((List)currentObj).remove(
                            levelArray[ maxOfLevel - 1 ][1] );
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
    
