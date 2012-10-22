package edu.ucla.mbi.proxy.actions;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * NativeServerConfigure Action:
 *
 *========================================================================= */

import com.opensymphony.xwork2.ActionSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.*;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.util.JsonContext;
import edu.ucla.mbi.util.struts2.action.PageSupport;

public class NativeServerConfigure extends PageSupport {

    private Log log = LogFactory.getLog( NativeServerConfigure.class );
    private NativeRestServer nativeRestServer;
    private Map<String, Object> restServer = new HashMap<String, Object>();
 
    //*** setter 
    public void setRestServer ( Map<String, Object> map ) {
        this.restServer = map;
    }

    public void setNativeRestServer( NativeRestServer server ) {
        this.nativeRestServer = server;
    }    

    //*** getter
    public NativeRestServer getNativeRestServer() {
        return nativeRestServer;
    }

    public Map<String,Object> getRestServer() {
        return restServer;
    }

    //---------------------------------------------------------------------
    // operations: op.xxx
    //----------- 

    private Map<String,String> opm ; //LS

    public void setOp( Map<String,String> op ) {
        this.opm = op;
    }

    public Map<String,String> getOp(){
        return opm;
    }

    //---------------------------------------------------------------------

    private Map<String,String> opp;  // params

    public void setOpp( Map<String,String> opp ) {
        this.opp = opp;
    }

    public Map<String,String> getOpp(){
        return opp;
    }

    //---------------------------------------------------------------------

    public String execute() throws Exception {

        log.info( " NativeConfigureAction execute..." );
        log.info( "execute: opm=" + opm + " and opp=" + opp );
        
        super.findMenuPage();

        if( getId() != null && getId().equals("json") ){
            log.info( "format is json. ");

            restServer = (Map<String, Object>)nativeRestServer
                            .getRestServerContext().getJsonConfig()
                                                    .get("restServer");

            return "json";
        }
        
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

                    if( getOpp() != null ) {
                        parseAndUpdateJsonWithOpp();
                    }

                    return "rest-server";           
                }

            }
        
        }

        log.info( "execute: return fault.");

        return "fault";
    }

    private void parseAndUpdateJsonWithOpp () throws ProxyFault {

        Map<String, Object> jrs =
                (Map)nativeRestServer.getRestServerContext()
                                        .getJsonConfig().get( "restServer");

        for( String oppKey:opp.keySet() ) {
            
            String[] oppKeyArray = oppKey.split( "_" );
            String provider= oppKeyArray[0];
            String service = oppKeyArray[1];
            String serverKey = oppKeyArray[2];

            Map<String, Object> jsonProviderMap = 
                    (Map<String, Object>)jrs.get(provider);

            if( jsonProviderMap != null ) {

                Map<String, Object> jsonServiceMap =
                    (Map<String, Object>) jsonProviderMap.get(service);

                if( jsonServiceMap != null ) {

                    String jsonServerValue = 
                        (String)(((ArrayList)jsonServiceMap
                                                .get(serverKey)).get(0));

                    if( jsonServerValue != null ) {
                        //*** update serverValue in Json object
                        ( (Map<String, Object>) ( (Map<String, Object>)
                            jrs.get(provider) ).get(service) )
                                .put( serverKey, Arrays.asList(
                                                getOpp().get( oppKey) ) );
                    } else {
                        //*** add new server Key/value pair in Json object
                        ArrayList<String> serverValueList = new ArrayList();
                        serverValueList.add( getOpp().get( oppKey ) );

                        Map<String, ArrayList> jsonServerMap = new HashMap();

                        jsonServerMap.put( serverKey, serverValueList );

                        jsonServiceMap.put( service, jsonServerMap );
                    } 
                } else {
                    //*** add new service in Json object
                    ArrayList<String> serverValueList = new ArrayList();
                    serverValueList.add( getOpp().get( oppKey ) );

                    Map<String, ArrayList> jsonServerMap = new HashMap();

                    jsonServerMap.put( serverKey, serverValueList );

                    jsonServiceMap.put( service, jsonServerMap );
                }
            } else {
                //*** add new provider in Json object        
                ArrayList<String> serverValueList = new ArrayList();
                serverValueList.add( getOpp().get( oppKey ) );

                Map<String, ArrayList> jsonServerMap = new HashMap();

                jsonServerMap.put( serverKey, serverValueList );

                Map<String, Object>  jsonServiceMap = new HashMap();
                jsonServiceMap.put( service, jsonServerMap );
            
                jrs.put( provider, jsonServiceMap);
                
            }
        }        

        nativeRestServer.getRestServerContext().getJsonConfig()
                                                .put("restServer", jrs );

        //*** update config
        if( super.doJsonFileUpdate( nativeRestServer.getRestServerContext(),
                                    nativeRestServer.getRestServerJFP() ) )
        {
            nativeRestServer.configInitialize();
        }
    }
}
