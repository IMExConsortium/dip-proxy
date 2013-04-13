package edu.ucla.mbi.proxy.struts.action;

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

import edu.ucla.mbi.util.context.JsonContext;
import edu.ucla.mbi.util.struts.action.PortalSupport;
import org.json.*;

public class CacheStatus extends PortalSupport {

    private WSContext wsContext = null;

    //private Map<String,Map> nativeCounts = null;
    //private Map<String,Map> dxfCounts = null;
    private Map<String, Long> nativeCounts = null;
    private Map<String, Long> dxfCounts = null;     
    private Map<String,String> op  = null;

    public void setOp( Map<String, String> op ) {
        this.op = op;
    }

    public Map<String, String> getOp() {
        return op;
    }

    public void setWsContext( WSContext wsContext ) {
        this.wsContext = wsContext;
    }
    
    //---------------------------------------------------------------------

    /*
    public Map<String,Map> getCounts() {
        return nativeCounts;
    }*/

    //public Map<String,Map> getNativeCounts() {
    public Map<String, Long> getNativeCounts() {
        return nativeCounts;
    }

    //public Map<String,Map> getDxfCounts() {
    public Map<String, Long> getDxfCounts() {
        return dxfCounts;
    }

    //---------------------------------------------------------------------

    public String execute() throws Exception {

        Log log = LogFactory.getLog( CacheStatus.class );
        log.info("CacheStatus execute ...");
        
        //nativeCounts = new TreeMap<String,Map>();
        //dxfCounts = new TreeMap<String,Map>();
        nativeCounts = new TreeMap<String, Long>();
        dxfCounts = new TreeMap<String, Long>();        

        Set<String> providers = wsContext.getServices().keySet();

        log.info( "getOp()=" + getOp() );

        try {

            NativeRecordDAO ndo = wsContext.getDipProxyDAO().getNativeRecordDAO();

            if ( ndo != null ){

                // go over providers
                //------------------
                
                for (Iterator<String> ii = providers.iterator();
                     ii.hasNext(); ) {
                    
                    String prv = ii.next();

                    log.info( "prv=" + prv );

                    if( getOp() != null ) {
                        log.info( "op is not null. " );
                        for( String opKey:getOp().keySet() ) {
                            if( opKey.equals( "nativeremove" + prv  ) ) {
                                //call removeAll
                                log.info( "call nativeDAO removeAll." );
                                ndo.removeAll( prv );
                            }

                            if ( opKey.equals( "nativeexpire" + prv ) ) {
                                //call expireAll
                                log.info( "cal nativeDAO expireAll. " );
                                ndo.expireAll( prv );
                            }
                        }
                    }
                    
                    Long prvCounts = ndo.countAll( prv ) ;
                    if( prvCounts != null ) {
                        nativeCounts.put( prv, prvCounts );
                    }
                }
            }

        } catch ( DAOException de ) {
            log.warn( "DXOException: " + de.toString() );    
        }

        try {

            DxfRecordDAO ddo = wsContext.getDipProxyDAO()
                .getDxfRecordDAO();
            if ( ddo != null ){
                
                // go over providers
                //------------------

                //Set<String> providers = wsContext.getServices().keySet();

                for (Iterator<String> ii = providers.iterator();
                     ii.hasNext(); ) {

                    String prv = ii.next();

                    log.info( "prv=" + prv );

                    if( getOp() != null ) {

                        for( String opKey:getOp().keySet() ) {
                            if( opKey.equals( "dxfremove" + prv  ) ) {
                                //call removeAll
                                log.info( "ddo call removeAll. " );
                                ddo.removeAll( prv );
                            }

                            if ( opKey.equals( "dxfexpire" + prv ) ) {
                                //call expireAll
                                log.info( "ddo call expireAll. " );
                                ddo.expireAll( prv );
                            }
                        }
                    }

                    Long prvCounts = ddo.countAll( prv ) ;
                    if( prvCounts != null ) {
                        dxfCounts.put( prv, prvCounts );
                    }
                }
            }
           
        }catch( DAOException de ) {
            log.warn( "DAOException: " + de.toString() );
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
