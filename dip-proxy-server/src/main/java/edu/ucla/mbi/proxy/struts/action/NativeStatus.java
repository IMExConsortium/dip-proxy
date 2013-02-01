package edu.ucla.mbi.proxy.struts.action;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * NativeStatus Action:
 *
 *========================================================================= */

import com.opensymphony.xwork2.ActionSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import edu.ucla.mbi.server.WSContext;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.orm.*;
import edu.ucla.mbi.cache.orm.*;

import edu.ucla.mbi.util.context.JsonContext;
import edu.ucla.mbi.util.struts.action.PageSupport;
import org.json.*;

public class NativeStatus extends PageSupport {

    private Log log = LogFactory.getLog( NativeStatus.class );

    private WSContext wsContext = null;

    private Map<String,Map> delays = null;


    public void setWsContext( WSContext wsContext ) {
        this.wsContext = wsContext;
    }

    public Map<String,Map> getDelays() {
        return delays;
    }
    

    public String execute() throws Exception {

        log.info( " NativeStatus execute..." );

        super.findMenuPage();

        delays = new TreeMap<String,Map>();

        try {
            NativeAuditDAO ado = DipProxyDAO.getNativeAuditDAO();

            // go over providers
            //------------------

            Set<String> providers = wsContext.getServices().keySet();

            for (Iterator<String> ii = providers.iterator();
                ii.hasNext(); ) {

                String prv = ii.next();

                log.info( "prv=" + prv );

                Map<String,Double> prvDelay = ado.delayAll( prv );
                log.info( "prvDelay end. ");
                delays.put( prv, prvDelay );
            }
        } catch ( DAOException de ) {

        }

        return SUCCESS;
    }

    /**
     * Provide default value for Message property.
     */
    public static final String MESSAGE = "foo.message";

    /**
     * Field for Message property.
     */
    private String message;

    /**
     * Return Message property.
     *
     * @return Message property
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set Message property.
     *
     * @param message Text to display on HelloWorld page.
     */
    public void setMessage(String message) {
        this.message = message;
    }
}