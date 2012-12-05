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


        String[] levelArrayT = new String[contextDepth];
        String[] levelArrayI = new String[contextDepth];


        int maxOfLevel = 0; // this value <= contextDepth

        //*** fill levelArray using oppVal
        for( String oppKey:getOpp().keySet() ) {
            String oppVal = getOpp().get( oppKey ); // mN|lN

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
                setKey = oppVal;
            } 

            if ( oppKey.equals( "value" ) ) {
                setVal = oppVal;
            }
        }
        
        //*** validate levelArray

        boolean pathOk = false;
        int pathDpt = 0;
        
        for( int i = maxOfLevel -1; i >0; i-- ) { 
            if( pathOk ){
                if( levelArrayT[i] == null || levelArrayI[i] == null ){
                    pathOk = false;
                    break;
                }
            } else {
                if( levelArrayT[i] != null || levelArrayI[i] != null ){
                    pathOk = true;
                    pathDpt = i;
                }
            }
        } 
        if( !pathOk ){
            // bad path
        }
        
        boolean isNew = false; // check if the level is new added
        Object currentObj = null;
        Object parentObj = null;
        String currentLevelType = null;  
        String parentLevelType = null;  

        
        Collection currentColl = null;
        
        for( int i = 0; i < pathDpt; i++ ) {
            if( currentColl==null ){
                currentColl = contextMap.get( contextTop );
            } else {
                if( levelArrayT[i].equals("m") ){
                    try{ 
                        currentColl = (Map) currentColl.get(levelArrayI[i]);
                    } catch(Exception ex) {
                        // path error
                    }
                }
                if(levelArrayT[i].equals("l") ){
                    try{
                        int index = Integer.valueOf(levelArrayI[i]);
                        currentColl = (List) currentColl.get( index );
                    } catch(Exception ex) {
                        // path error
                    }
                }
            }
        }

        // currentColl: not null collection to perform operation upon
        // opKey:  add|set|drop
        // opVal:  map|list|value 
        // newKey
        // newVal
        
        if( opKey.equals("add")){

            Collection ncoll = null;
            if(opVal.equals("map")){
                ncoll = new HashMap();
            }
            if(opVal.equals("list")){
                ncoll = new ArrayList();
            }
            
            if( currentColl instanceOf Map){
                ((Map) currentColl).put( newKey, ncoll);
            } 
            
            if( currentColl instanceOf List){
                ((List)currentList).add( ncoll );
            } 
        } else {
            
            Object co = null;
            if( currentColl instanceOf Map){
                co = ((Map)currentColl).get(newKey);                
            }
            if( currentColl instanceOf List){
                try{
                    co = ((List)currentColl).get(Integer.valueOf(newKey));
                } catch(Exception ex){
                    //error
                }
            }
            
            if( opKey.equals("set")){

                if( co == null ||  co instanceOf String ){
                
                    if(currentColl instanceOf Map){
                        ((Map)currentColl).put(newKey,newVal);
                    }
                    if(currentColl instanceOf List){
                        try{
                            integer index = Integer.valueOf(newKey);
                            for( int i=((List)currentColl).size();
                                 i<=index; i++){
                                ((List)currentColl).add( null );
                            }
                            ((List)currentColl).set( index, newVal );
                        }catch(Exception ex){
                            // error
                        }       
                    }
                }
            }
            
            if( opKey.equals("drop")){
                
                if( (co instaceOf Map && !opVal.equals("map"))
                    ||(co instaceOf List && !opVal.equals("list"))
                    ||(co instaceOf String && !opVal.equals("value"))){
                
                    //error: wrong object to drop
                }

                if( currentColl instanceOf Map ){
                    ((Map)currentColl).remove( newKey );
                }
                if( currentColl instanceOf List ){
                    ((List)currentColl).remove( Integer.valueOf(newKey) );
                }
            }
        }

        boolean updateJson = true;

        if( updateJson ) {
            saveJsonContext();
        } 
            
        return SUCCESS;
        
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
    
