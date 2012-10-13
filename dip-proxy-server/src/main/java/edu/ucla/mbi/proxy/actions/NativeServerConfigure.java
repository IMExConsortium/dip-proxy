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

    private String buttonName = "View";
    
    public void setNativeRestServer( NativeRestServer server ) {
        this.nativeRestServer = server;
    }    

    public void setButtonName ( String name ) {
        this.buttonName = name;
    }

    public NativeRestServer getNativeRestServer() {
        return nativeRestServer;
    }

    public String getButtonName () {
        return buttonName;
    }


    public String execute() throws Exception {

        log.info( " NativeConfigureAction execute..." );

        super.findMenuPage();

        if( buttonName.equals( "Update" ) ) {
            if( super.doJsonFileUpdate( nativeRestServer.getRestServerContext(),
                                        nativeRestServer.getRestServerJFP() ) ) 
            {
                nativeRestServer.configInitialize();
                return "rest-server";
            } 
        } 
        
        if( buttonName.equals( "Clear" ) ) {
            return "rest-server-clear";
        } 

        if( buttonName.equals( "View" ) ) {
            return "rest-server";
        }

        return "fault";
    }
}
