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

import edu.ucla.mbi.fault.*;

/*
import edu.ucla.mbi.server.WSContext;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.orm.*;
import edu.ucla.mbi.cache.orm.*;
*/

import edu.ucla.mbi.proxy.*;

import edu.ucla.mbi.util.JsonContext;
import edu.ucla.mbi.util.struts2.action.PageSupport;
import org.json.*;

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

        Map<String,Object> jpd = super.getPageContext().getJsonConfig();

        if ( getId() != null && getId().length() > 0 ) {

            Map pages = (Map) ((Map) jpd.get("pageConfig") ).get( "pages" );
            page = (Map) pages.get( getId() );

            log.info("PageSupport: id=" + getId() );

            if ( page != null ) {
                log.info( "page=" + page );

                log.info(" PageAction: title=" + page.get( "title" ) +
                         " menusel=" + page.get( "menusel" ) +
                         " menudef=" + page.get( "menudef" ) );

                // default tab selection
                //----------------------

                if ( getMst() == null || getMst().length() == 0 ) {
                    setMst( (String) page.get( "menusel" ) );
                    if ( getMst() == null || getMst().length() == 0 ) {
                        setMst( "" );
                    }
                }
            } else {
                addActionError( "No page found" );
            }
        }  else {
            addActionError( "No page id" );
        }
        
        if( buttonName.equals( "Update" ) ) {
            doRestServerUpdate();
        } else if( buttonName.equals( "Clear" ) ) {
            return "rest-server-clear";
        } else {
            addActionError( "No page id" );
        }

        return SUCCESS;
    }

    public void doRestServerUpdate() throws ProxyFault {
        
        JsonContext restServerContext = nativeRestServer.getRestServerContext();
        /*
        Map<String, Object> restServerMap = restServerContext.getJsonConfig();

        Map<String, Object> providerMap = (Map)restServerMap.get("restServer");

        String[] prolinksAcTag = (String[]) ((Map)((Map)providerMap.get("MBI")).get("prolinks")).get("restAcTag");

        log.info( "doRestSererUpdate: prolinksAcTag=" + prolinksAcTag[0] );
        */
        PrintWriter pw = null;

        try {
            pw = new PrintWriter ( new File ( nativeRestServer.getRestServerJFP() ) );
            synchronized(this) {
                log.info( "doRestServerUpdate: before write JsonConfigDef. " );
                restServerContext.writeJsonConfigDef( pw );
            }

            pw.flush();
        } catch ( Exception e ) {
            log.info ( "JSON printting error: " + e.toString() );
            throw FaultFactory.newInstance ( Fault.JSON_CONFIGURATION );//json configure file fault
        } finally {
            if( pw != null ){
                pw.close();
            }
        }
        
        nativeRestServer.configInitialize();
    }
    
     /**
     * Provide default value for Message property.
     */
    //public static final String MESSAGE = "foo.message";

    /**
     * Field for Message property.
     */
    //private String message;

    /**
     * Return Message property.
     *
     * @return Message property
     */
    /*
    public String getMessage() {
        return message;
    }*/

    /**
     * Set Message property.
     *
     * @param message Text to display on HelloWorld page.
     */
    /*
    public void setMessage(String message) {
        this.message = message;
    }
    */
}
