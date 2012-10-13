package edu.ucla.mbi.proxy.actions;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * CacheStatus Action:
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

import edu.ucla.mbi.util.JsonContext;
import edu.ucla.mbi.util.struts2.action.PageSupport;
import org.json.*;

public class CacheStatus extends PageSupport {

    private WSContext wsContext = null;

    private Map<String,Map> counts = null;
        

    public void setWsContext( WSContext wsContext ) {
        this.wsContext = wsContext;
    }
    
    //---------------------------------------------------------------------

    public Map<String,Map> getCounts() {
        return counts;
    }

    //---------------------------------------------------------------------

    public String execute() throws Exception {

        Log log = LogFactory.getLog( CacheStatus.class );
        log.info("CacheStatus execute ...");

        super.findMenuPage();
 
        counts = new TreeMap<String,Map>();
        
        try {

            NativeRecordDAO ndo = DipProxyDAO.getNativeRecordDAO();

            if ( ndo != null ){

                // go over providers
                //------------------
                
                Set<String> providers = wsContext.getServices().keySet();
                
                for (Iterator<String> ii = providers.iterator();
                     ii.hasNext(); ) {
                    
                    String prv = ii.next();

                    log.info( "prv=" + prv );
                    
                    Map<String,Long> prvCounts = ndo.countAll( prv );
                    counts.put( prv, prvCounts );
                }
            }

        } catch ( DAOException de ) {
            
        }
        
        return "success";
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
