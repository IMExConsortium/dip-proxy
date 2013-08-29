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

import edu.ucla.mbi.proxy.context.WSContext;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.orm.*;
import edu.ucla.mbi.cache.orm.*;

import edu.ucla.mbi.util.context.JsonContext;
import edu.ucla.mbi.util.struts.action.PortalSupport;
import org.json.*;

public class CacheStatus extends PortalSupport {

    private WSContext wsContext = null;

    private Map<String,Map> nativeCounts = null;
    private Map<String,Map> dxfCounts = null;
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

    public Map<String,Map> getNativeCounts() {
        return nativeCounts;
    }

    public Map<String,Map> getDxfCounts() {
        return dxfCounts;
    }

    //--------------------------------------------------------------------------

    public String execute() throws Exception {

        Log log = LogFactory.getLog( CacheStatus.class );
        log.info("CacheStatus execute ...");
        
        nativeCounts = new HashMap<String,Map>();
        dxfCounts = new HashMap<String,Map>();

        Set<String> providers = wsContext.getProviderSet();

        log.info( "getOp()=" + getOp() );

        for (Iterator<String> i = providers.iterator();
             i.hasNext(); ) {
        
            String prv = i.next();     
            log.info( "prv=" + prv );
        
            // go over providers
            //------------------

            Set<String> services = wsContext.getServiceSet( prv );

            NativeRecordDAO ndo = 
                wsContext.getDipProxyDAO().getNativeRecordDAO();
            
            if ( ndo != null ){

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
                    
                Map<String, Long> nativeMap = new HashMap<String, Long>();
                    
                for (Iterator<String> ii = services.iterator();
                     ii.hasNext(); ) {

                    String service = ii.next();

                    Long serviceCounts = ndo.countAll( prv, service ) ;
                    nativeMap.put( service, serviceCounts );
                }

                nativeCounts.put( prv, nativeMap );
            }

            DxfRecordDAO ddo = wsContext.getDipProxyDAO().getDxfRecordDAO();

            if ( ddo != null ){
                
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
                    
                Map<String, Long> dxfMap = new HashMap<String, Long>();
                    
                for (Iterator<String> ii = services.iterator();
                     ii.hasNext(); ) {

                    String service = ii.next();

                    Long serviceCounts = ddo.countAll( prv, service ) ;
                    if( serviceCounts != null ) {
                        dxfMap.put( service, serviceCounts );
                    }
                }
                    
                dxfCounts.put( prv, dxfMap );

            }
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
