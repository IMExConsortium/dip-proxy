package edu.ucla.mbi.proxy.actions;

/*===========================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-#$
 * $Id:: NativeStatus.java 2609 2012-08-01 00:04:54Z wyu                    $
 * Version: $Rev:: 2609                                                     $
 *===========================================================================
 *
 * NativeStatus Action:
 *
 *========================================================================= */

import com.opensymphony.xwork2.ActionSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/*
import edu.ucla.mbi.server.WSContext;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.orm.*;
import edu.ucla.mbi.cache.orm.*;
*/

import edu.ucla.mbi.proxy.NativeRestServer;

import edu.ucla.mbi.util.JsonContext;
import edu.ucla.mbi.util.struts2.action.PageSupport;
import org.json.*;

public class NativeConfigureAction extends PageSupport {
    private Log log = LogFactory.getLog( NativeStatus.class );

    private NativeRestServer nativeRestServer;
    
    public void setNativeRestServer( NativeRestServer server ) {
        this.nativeRestServer = server;
    }    

    public NativeRestServer getNativeRestServer() {
        return nativeRestServer;
    }

    public String exceute() throws Exception {
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

        return SUCCESS;
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
