package edu.ucla.mbi.proxy.actions;

/*==============================================================================
 * $HeadURL:: https://imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-proxy-s#$
 * $Id:: NativeServerConfigure.java 2812 2012-11-07 23:14:55Z lukasz           $
 * Version: $Rev:: 2812                                                        $
 *==============================================================================
 *
 * JsonContextConfigure Action:
 *
 *=========================================================================== */

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
    private final String STRING = "string";

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

        String newKey = null;
        String newVal = null;


        String[] levelArrayT = new String[contextDepth];
        String[] levelArrayI = new String[contextDepth];


        int maxOfLevel = 0; // this value <= contextDepth

        //*** fill levelArray using oppVal
        for( String oppKey:getOpp().keySet() ) {
            String oppVal = getOpp().get( oppKey ); // mN|lN
            log.info( "oppKey=" + oppKey + " and oppVal=" + oppVal );

            if( oppKey.startsWith( "m" ) || oppKey.startsWith( "l" ) ) {
                //*** m means map and l means list
                int level;

                try {
                    level = Integer.valueOf( oppKey.substring(1) ).intValue();
                    log.info( "level=" + level );
                } catch ( Exception e ) {
                    log.warn( "op: oppKey(" + oppKey + ") format wrong. " );
                    return ERROR;
                }

                if( level > 0 && level <= contextDepth ) {

                    levelArrayI[level -1] = oppVal;
                    if( oppKey.startsWith( "m" ) ) {
                        levelArrayT[level -1] = "m";
                    } 

                    if( oppKey.startsWith( "l" ) ) {
                        levelArrayT[level -1] = "l";
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
                newKey = oppVal;
                log.info( "newKey coming in with " + newKey );
            } 

            if ( oppKey.equals( "value" ) ) {
                newVal = oppVal;
            }
        }
        
        //*** validate levelArray

        boolean pathOk = false;
        int pathDpt = 0;
        
        for( int i = maxOfLevel; i > 0; i-- ) { 
            log.info( "path i=" + i + " and pathOk=" + pathOk );
            if( pathOk ) {
                if( levelArrayT[i-1] == null || levelArrayI[i-1] == null ) {
                    pathOk = false;
                    break;
                }
            } else {
                if( levelArrayT[i-1] != null || levelArrayI[i-1] != null ) {
                    pathOk = true;
                    pathDpt = i ;
                }
            }
        }
 
        if( !pathOk && pathDpt > 0) {
            log.warn( " wrong level path in the url request. " );
            return ERROR;
        }
        
        //Object currentObj = null;
        Object currentObj = contextMap.get( contextTop );
        boolean updateJson = false;

        //***
        // currentObj: not null collection to perform operation upon
        // opKey:  add|set|drop
        // opVal:  map|list|value 
        // newKey
        // newVal
        //***

        log.info( "pathDpt=" + pathDpt );
        for( int i = 0; i < pathDpt; i++ ) {
        
            if( levelArrayT[i].equals("m") ) {
                try {
                    currentObj = ((Map)currentObj).get( levelArrayI[i] );
                } catch ( Exception ex ) {
                    log.warn( "The map level" + i + " =" + levelArrayI[i] +
                              " does not match with the json file. " );
                    return ERROR;
                }

                if( currentObj == null ) {
                    log.warn( "The map level" + i + " =" + levelArrayI[i] +
                              " does not match with the json file. " );
                    return ERROR;
                }
            }

            if( levelArrayT[i].equals("l") ) {
                try {
                    int index = Integer.valueOf( levelArrayI[i] );
                    currentObj = ((List)currentObj).get( index );
                } catch( Exception ex ) {
                    log.warn( "The list level" + i + " =" + levelArrayI[i] +
                              " does not match with the json file. " ); 
                    return ERROR;
                }
            }
        }
  
        if( opKey.equals("add") ) {

            Object nextObj = null;

            if( opVal.equals("map") ) {
                nextObj = new HashMap();
            }
            if( opVal.equals("list") ) {
                nextObj = new ArrayList();
            }
            
            if( currentObj instanceof Map ) {
                log.info( "currentObj is Map, and newKey=" + newKey );
                ((Map)currentObj).put( newKey, nextObj );
                log.info( "currentObj after put is " + currentObj );
                updateJson = true;
            } 
            
            if( currentObj instanceof List ) {
                log.info( "currentObj is List. " );
                if( newKey != null && newKey.matches("([0]|[1-9][0-9]*)" ) ) {
                    int index = Integer.valueOf( newKey );
                    for( int i=((List)currentObj).size(); i<=index; i++ ){
                        ((List)currentObj).add( null );
                        log.info( i+ " null=" + ((List)currentObj).get(i));

                        log.info( i+ " if(null==null)=" + (((List)currentObj).get(i) == null) );
                    }
                    ((List)currentObj).set( index, nextObj );
                } else {
                    ((List)currentObj).add( nextObj ); // only add list at the end of the parent list
                }
                updateJson = true;
            } 
        } else {
            
            Object co = null;

            if( currentObj instanceof Map ) {
                co = ((Map)currentObj).get( newKey );                
            }

            if( currentObj instanceof List ) {
                int index = Integer.valueOf( newKey );
                log.info( "index=" + index + " and list size=" + 
                          ((List)currentObj).size() );
                if( index < ((List)currentObj).size() ) {
                    try{
                        co = ((List)currentObj).get( index );
                    } catch( Exception ex ) {
                        log.warn( "The newKey=" + newKey + " for the list " +
                                  "index does not match with the json file. " );
                        return ERROR;
                    }
                }
            }
            
            if( opKey.equals("set") ) {

                if( co == null ||  co instanceof String ){

                    if( currentObj instanceof Map ) {
                        ((Map)currentObj).put( newKey, newVal );
                        updateJson = true;
                    }
            
                    if( currentObj instanceof List ) {
                        log.info( "currentObj is List. " );
                        try {
                            int index = Integer.valueOf( newKey );
                            for( int i=((List)currentObj).size();
                                 i<=index; i++ ) 
                            {
                                ((List)currentObj).add( null );
                            }
                            ((List)currentObj).set( index, newVal );
                            updateJson = true;
                        } catch( Exception ex ) {
                            log.warn( "The newKey=" + newKey + "for the list " +   
                                      "index does not match with the json file. " );
                            return ERROR;
                        }       
                    }
                }
            }
            
            if( opKey.equals("drop")){
                log.info( "op is drop. " );                
                if( ( co instanceof Map && !opVal.equals("map") )
                    ||( co instanceof List && !opVal.equals("list") ) )
                {
                    log.warn( "The operation drop " + opVal + " does not " +
                              "match with the json file. " );

                    return ERROR;
                }

                if( currentObj instanceof Map ){
                    ((Map)currentObj).remove( newKey );
                    updateJson = true;
                }
                if( currentObj instanceof List ) {
                    int index = Integer.valueOf( newKey );
                    log.info( "drop index=" + Integer.valueOf( newKey ) );
                    ((List)currentObj).remove( index );
                    log.info( "after remove index. " );
                    updateJson = true;
                }
            }
        }

        if( updateJson ) {
            saveJsonContext();
        } 
            
        return SUCCESS;
        
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
    
