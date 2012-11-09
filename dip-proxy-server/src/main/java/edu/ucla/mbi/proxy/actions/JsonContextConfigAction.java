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
    
   
    private JsonContext restServerContext;
    private NativeRestServer nativeRestServer;
   
    private Map<String, Object> jsonContext; 
 
    //*** setter
    public void setRestServerContext( JsonContext context ) {
        this.restServerContext = context;
    }

    public void setNativeRestServer( NativeRestServer nativeRestServer ) {
        this.nativeRestServer = nativeRestServer;
    }

    //*** getter
    public Map<String,Object> getRestServer() {
        return (Map<String, Object>)jsonContext.get("restServer");
    }
     
    public Map<String, Object> getJsonContext() {
        return jsonContext;
    } 

    //---------------------------------------------------------------------

    public String execute() throws Exception {

        log.info( " JsonContextConfigAction execute..." );
        
        //super.findMenuPage();
        /*
        if( getId() != null && getId().equals("restserver") ){
            return "rest-server";
        }*/
        /*
        if( getId() != null && getId().equals("json") ){
            log.info( "format is json. ");

            restServer = (Map<String, Object>)nativeRestServer
                            .getRestServerContext().getJsonConfig()
                                                    .get("restServer");

            return "json";
        }
        */

        String jsonConfigFile =
                (String) restServerContext.getConfig().get( "json-config" );

        String srcPath = getServletContext().getRealPath( jsonConfigFile );

        try {
            restServerContext.readJsonConfigDef( srcPath );
        } catch( Exception e ) {
            log.info( "configInitialize exception: " + e.toString() );
            throw FaultFactory.newInstance ( 27 ); // json configuration
        }

        jsonContext = restServerContext.getJsonConfig();

        if( getOp() == null ) {
            log.info( "execute: enter op=view.");
            return "rest-server";
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
                        for( String oppKey: getOpp().keySet() ) {
                            log.info( "execute: oppKey=" + oppKey );
                            getOpp().put(oppKey, "");  
                        }
                        parseAndUpdateJsonWithOpp();
                    }
                    
                    return "rest-server";
                }
       
                 
                if( key.equalsIgnoreCase( "update" ) ) {
                    
                    log.info( "execute: op.update hit." );

                    // check if there is a new service added
                    if( getOpp().get("newProvider") != null
                            && getOpp().get("newService") != null
                            && getOpp().get("newRestUrl") != null
                            && getOpp().get("newRestAcTag") != null )
                    {
                        log.info( "update, but add needed. " );
                        addNewServiceToJson( false );
                    } else {
                        log.warn( "The new service info is not complete. " );
                        addActionError( "the new service info is not " +
                                            "complete, please fill fully." );
                    }

                    parseAndUpdateJsonWithOpp();

                    return "rest-server";           
                }

                if( key.equalsIgnoreCase( "add" ) ) {
                    log.info( "execute: op.add hit. " );

                    if( getOpp() != null ) {
                        if( getOpp().get("newProvider") != null
                                && getOpp().get("newService") != null
                                && getOpp().get("newRestUrl") != null
                                && getOpp().get("newRestAcTag") != null )
                        {
                            addNewServiceToJson( true );
                            return "rest-server";
                        } else {
                            log.warn( "The new service info is not complete. " );
                            addActionError( "the new service info is not " +
                                            "complete, please fill fully." );
                        }
                    }
                }

                if( key.equalsIgnoreCase( "show" ) ) {
                    // prepare
                    //setRet("rest-config"); 
                    log.info( "execute: op.show hit. " );
                    return "json";
                }

            } else {
                return "rest-server";
            }
            
        
        }

        log.info( "execute: return fault.");

        return SUCCESS;
    }

    private void addNewServiceToJson( boolean writeToJson ) throws ProxyFault {
   
        boolean isNew = false;

        log.info( " enter addNewServiceToJson. " );
    
        Map<String, Object> jsonProviderMap = 
            (Map<String, Object>)( (Map<String, Object>) jsonContext
                                        .get("restServer") )
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

        ArrayList<String> jsonRestUrl =
                (ArrayList)jsonServiceMap.get("restUrl");

        if( jsonRestUrl == null ) {
            jsonRestUrl = new ArrayList();
            jsonRestUrl.add( getOpp().get("newRestUrl") );
            jsonServiceMap.put( "restUrl", jsonRestUrl );
            isNew = true;
        } else {
            if( !jsonRestUrl.get(0).equals( getOpp().get("newRestUrl") ) ) {
                jsonRestUrl.set( 0,  getOpp().get("newRestUrl") );
                isNew = true; // actually it's a update
            }
        }
        
        ArrayList<String> jsonRestAcTag =
                (ArrayList)jsonServiceMap.get("restAcTag");

        if( jsonRestAcTag == null ) {
            jsonRestAcTag = new ArrayList();
            jsonRestAcTag.add( getOpp().get("newRestAcTag") );
            jsonServiceMap.put( "restAcTag", jsonRestAcTag );
            isNew = true;
        } else {
            if( !jsonRestAcTag.get(0).equals( getOpp().get("newRestAcTag") ) ) {
                jsonRestAcTag.set( 0, getOpp().get("newRestAcTag") );
                isNew = true; // actually it's a update
            }
        }

        log.info( "addNew: isNew=" + isNew );

        if( isNew ) {
            jsonProviderMap.put( getOpp().get("newService"),
                                 jsonServiceMap );

            
            //jrs.put( getOpp().get("newProvider"),
            //         jsonProviderMap);
           
            ((Map<String, Object>)jsonContext.get("restServer"))
                                    .put( getOpp().get("newProvider"),
                                          jsonProviderMap );

            //nativeRestServer.getRestServerContext()
            //                    .getJsonConfig().put("restServer", jrs );
            
            log.info( "addNew: writeToJson=" + writeToJson );

            if( writeToJson ) {
                //*** save to json file
                saveNativeServerConfigure();

                //*** reinitializing nativeRestServer using the updated json file
                nativeRestServer.configInitialize();
            }
        }
    
    }

    
    private void parseAndUpdateJsonWithOpp () throws ProxyFault {

        for( String oppKey:getOpp().keySet() ) {
           
            if( !oppKey.contains( "_" ) ) {
                continue;
            } 

            String[] oppKeyArray = oppKey.split( "_" );
            String provider= oppKeyArray[0];
            String service = oppKeyArray[1];
            String serverKey = oppKeyArray[2];

            Map<String, Object> jsonProviderMap = 
            //        (Map<String, Object>)jrs.get(provider);
                (Map<String, Object>) ( (Map<String, Object>)jsonContext
                                                .get("restServer") )
                                                    .get( "provider") ;

            if( jsonProviderMap == null ) {
                //*** create new provider in Json object        
                jsonProviderMap = new HashMap();
            }

            Map<String, Object> jsonServiceMap =
                    (Map<String, Object>) jsonProviderMap.get(service);

            if( jsonServiceMap == null ) {
                //*** create new service in Json object
                jsonServiceMap = new HashMap();
            }
                    
            ArrayList<String> jsonServerValue = 
                            (ArrayList)jsonServiceMap.get(serverKey);

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

            ((Map<String, Object>) jsonContext.get("restServer") )
                                        .put( provider, jsonProviderMap);

        }        
        
        //*** update config
        saveNativeServerConfigure();
        nativeRestServer.configInitialize();
    
    }
    
    private void saveNativeServerConfigure() throws ProxyFault {
            
        String jsonConfigFile = (String) restServerContext.getConfig()
                                                        .get( "json-config" );

        log.info( "saveNativeServerConfig: jsonConfigFile=" + jsonConfigFile );

        String srcPath =
            getServletContext().getRealPath( jsonConfigFile );

        log.info( "saveNativeServerConfig:  srcPath=" + srcPath );

        try { 
            restServerContext.writeJsonConfigDef( srcPath  );
            
        } catch ( Exception e ) {
            log.info( " saveNativeServerConfigure exception: " + e.toString() );
            throw FaultFactory.newInstance ( 27 ); // json configuration         
        }

        log.info( "saveNativeServerConfigure: after writing to json file. " ); 
    }
        
}
    
