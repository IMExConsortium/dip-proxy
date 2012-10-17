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

//public class NativeServerConfigure extends PageSupport {

public class NativeServerConfigure {

    private Log log = LogFactory.getLog( NativeServerConfigure.class );

    private NativeRestServer nativeRestServer;
    private String buttonName = "View";

    private String format;
    private Map<String, Object> restServer = new HashMap<String, Object>();
 
    //*** setter 
    public void setFormat( String format ) {
        this.format = format;
    }

    public void setRestServer ( Map<String, Object> map ) {
        this.restServer = map;
    }

    public void setNativeRestServer( NativeRestServer server ) {
        this.nativeRestServer = server;
    }    

    public void setButtonName ( String name ) {
        this.buttonName = name;
    }

    //*** getter
    public NativeRestServer getNativeRestServer() {
        return nativeRestServer;
    }

    public String getButtonName () {
        return buttonName;
    }

    public Map<String,Object> getRestServer() {
        return restServer;
    }

    //---------------------------------------------------------------------
    // operations: op.xxx
    //----------- 

    private Map<String,String> opm;

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
        
        //super.findMenuPage();

        /*

        if( opm != null & opm.get("update") != null ){
            // update config


            
        }


        if( buttonName.equals( "Update" ) ) {
            if( super.doJsonFileUpdate( nativeRestServer.getRestServerContext(),
                                        nativeRestServer.getRestServerJFP() ) ) 
            {
                nativeRestServer.configInitialize();
                return "rest-server";
            } 
        } 



       // <input type="submit" name="op.clear" value="XXXX" />
      
        if( opm != null & opm.get("clear") != null ){  
            // do somethin  
            return "rest-server-clear";
        }

        if( buttonName.equals( "Clear" ) ) {
            return "rest-server-clear";
        } 

        if( buttonName.equals( "View" ) ) {
            return "rest-server";
        }
        */
        //*** if called as native-configure?format=json should return configuration data as json
        
        if(format != null && format.equals("json") ){
            log.info( "format is json. "); 
            
            restServer = (Map<String, Object>)nativeRestServer
                            .getRestServerContext().getJsonConfig()
                                                    .get("restServer");
            
            return "json";
        }
        
        return "fault";
    }
}
