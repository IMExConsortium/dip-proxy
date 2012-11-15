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
     
    private JsonContext jsonContext;
    
    private Map<String, Object> contextMap; 
    private String contextTop; 

    //*** setter
    public void setJsonContext( JsonContext context ) {
        this.jsonContext = context;
    }
 
    //*** getter    
    public Map<String, Object> getContextMap() {
        return contextMap;
    } 

    public void setContextTop( String top) {
        contextTop = top;
    }
    
    public String getContextTop() {
        return contextTop;
    }

    int contextDepth = 1;

    public void setContextDepth( int depth) {
        contextDepth = depth;
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

        log.info( "execute: contextMap=" + contextMap );

        Set<String> cmKeySet = (Set<String>) contextMap.keySet();
        if( cmKeySet != null && cmKeySet.size() == 1 ) {
            String[] cmka = cmKeySet.toArray( new String[0] );
            contextTop = cmka[0];
            log.info( " execute: contextTop=" + contextTop );
        }
        
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
                
                if( key.equalsIgnoreCase( "clear" ) ) {
                    log.info( "execute: op.clear hit." );
                    
                    if( getOpp() != null ) {
                        clearJsonWithOpp();
                    }
                    
                    return SUCCESS;
                }
                
                if( key.equalsIgnoreCase( "update" ) 
                    || key.equalsIgnoreCase( "add" ) ) 
                {
                    
                    log.info( "execute: op.update hit." );

                    // check if there is a new service added
                    if( getOpp().get("newProvider") != null
                            && getOpp().get("newService") != null 
                            && getOpp().get("newProperty") != null
                            && getOpp().get("newValue") != null
                            && !getOpp().get("newProvider").equals( "" ) 
                            && !getOpp().get("newProperty").equals( "" )
                            && !getOpp().get("newProperty").equals( "" )
                            && !getOpp().get("newValue").equals( "" ) )
                    {
                        log.info( "update, but add needed also. " );

                        addNewServiceToJson(); //XXX

                        if(  key.equalsIgnoreCase( "add" ) ) {
                            return addNewServicePropertyToJson( true);
                        } else {
                            addNewServicePropertyToJson( false );
                        }
                    } 
                    parseAndUpdateJsonWithOpp();
                    return SUCCESS;           
                }

                if( key.equalsIgnoreCase( "show" ) ) {
                    log.info( "execute: op.show hit. " );
                    return "json";
                }

            } else {
                return SUCCESS;
            }        
        }

        log.info( "execute: return fault.");

        return ERROR;
    }

    private void addNewServiceToJson( ) throws ProxyFault {
   
        boolean isNew = false;

        log.info( " enter addNewServiceToJson. " );
    
        Map<String, Object> jsonProviderMap = 
            (Map<String, Object>)( (Map<String, Object>) contextMap
                                   .get( contextTop) )
                                    .get( getOpp().get("newProvider") ) ;

        if( jsonProviderMap == null ) {
            //*** create new provider in Json object        
            jsonProviderMap = new HashMap();
            isNew = true;
        }

        Map<String, Object> jsonServiceMap =
            (Map<String, Object>) jsonProviderMap
                    .get( getOpp().get("newService") );

        if( jsonServiceMap == null ) {
            //*** create new service in Json object
            jsonServiceMap = new HashMap();
            isNew = true;
        }
        
        log.info( "addNew: isNew=" + isNew );

        if( isNew ) {
            jsonProviderMap.put( getOpp().get("newService"),
                                 jsonServiceMap );
            
            ((Map<String, Object>) contextMap.get( contextTop))
                .put( getOpp().get( "newProvider" ),
                      jsonProviderMap );
        }
    }
    
    private String  addNewServicePropertyToJson( boolean writeToJson ) 
        throws ProxyFault {
        
        boolean isNew = false;

        log.info( " enter addNewServiceToJson. " );
    
        Map<String, Object> jsonProviderMap = 
            (Map<String, Object>)( (Map<String, Object>) contextMap
                                   .get( contextTop) )
            .get( getOpp().get("newProvider") ) ;

        if( jsonProviderMap == null ) {
            return ERROR;
        }

        Map<String, Object> jsonServiceMap =
            (Map<String, Object>) jsonProviderMap
            .get( getOpp().get( "newService" ) );

        if( jsonServiceMap == null ) {
            return ERROR;
        }

        ArrayList<String> property =
            (ArrayList) jsonServiceMap.get(getOpp().get( "newProperty" ) );
        
        if( property == null ) {
            property = new ArrayList();
            property.add( getOpp().get( "newValue" ) );
            isNew = true;
        } else {
            addActionError("the property(" + getOpp().get( "newProperty" ) +
                           ") has been existed. Please update it. ");
            return ERROR;
        }

        jsonServiceMap.put( getOpp().get( "newProperty" ), 
                            property );
        
        log.info( "addNew: isNew=" + isNew );

        if( isNew ) {
            jsonProviderMap.put( getOpp().get("newService"),
                                 jsonServiceMap );
            
            ((Map<String, Object>) contextMap.get( contextTop))
                .put( getOpp().get( "newProvider" ),
                      jsonProviderMap );
            
            log.info( "addNew: writeToJson=" + writeToJson );

            if( writeToJson ) {
                //*** save to json file
                saveJsonContext();
            }
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
            
            ArrayList<String> jsonServerValue = 
                (ArrayList) jsonServiceMap.get( serverKey );
            
            if( jsonServerValue == null ) {
                //*** create new jsonServerValue List in Json object
                jsonServerValue = new ArrayList();
                jsonServerValue.add( "" ) ;
            } else {
                //*** update jsonServerValue using opp value    
                jsonServerValue.set( 0, "" ) ;
            }
            
            jsonServiceMap.put( serverKey, jsonServerValue );
            jsonProviderMap.put( service, jsonServiceMap );
            
            ((Map<String, Object>) contextMap.get( contextTop) )
                .put( provider, jsonProviderMap);
            
        }        
        
    }
    
    private void parseAndUpdateJsonWithOpp () throws ProxyFault {

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
            
            ArrayList<String> jsonServerValue = 
                (ArrayList) jsonServiceMap.get( serverKey );
            
            if( jsonServerValue == null ) {
                //*** create new jsonServerValue List in Json object
                jsonServerValue = new ArrayList();
                jsonServerValue.add( getOpp().get( oppKey ) );
            } else {
                //*** update jsonServerValue using opp value    
                jsonServerValue.set( 0, getOpp().get( oppKey ) );
            }
            
            jsonServiceMap.put( serverKey, jsonServerValue );
            jsonProviderMap.put( service, jsonServiceMap );
            
            ((Map<String, Object>) contextMap.get( contextTop) )
                .put( provider, jsonProviderMap);
            
        }        
        
        //*** update config
        saveJsonContext();
    
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
    
