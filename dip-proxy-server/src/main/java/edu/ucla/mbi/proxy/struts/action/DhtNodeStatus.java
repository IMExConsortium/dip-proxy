package edu.ucla.mbi.proxy.struts.action;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * DhtNodeStatus Action:
 *
 *========================================================================= */

import com.opensymphony.xwork2.ActionSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.HashMap;


import ow.id.*;
import ow.dht.*;
import ow.messaging.*;
import ow.routing.*;

import edu.ucla.mbi.proxy.router.*;

import edu.ucla.mbi.util.context.JsonContext;
import edu.ucla.mbi.util.struts.action.PortalSupport;
import org.json.*;

public class DhtNodeStatus extends PortalSupport {

    private Log log = LogFactory.getLog(DhtNodeStatus.class);
    private Dht dht = null;

    private Map<String,String> nodeStatus = null;


    public void setDht( Dht dht ) {
        this.dht = dht;
    }
    
    public Map<String,String> getNodeStatus() {
        return nodeStatus;
    }

    //---------------------------------------------------------------------

    public String execute() throws Exception {

        log.info("DhtNodeStatus execute");

        //super.findMenuPage();

        DHTConfiguration dhtConf = dht.getDHT().getConfiguration();  
        RoutingService dhtRoutingService = dht.getDHT().getRoutingService();  
        IDAddressPair idap = dhtRoutingService.getSelfIDAddressPair();
       
        nodeStatus = new HashMap<String,String>(); 

        log.info( "dht=" + dht.getDHT() );
        
        log.info( "algorithm: " + dhtConf.getRoutingAlgorithm() );
        nodeStatus.put( "algorithm", dhtConf.getRoutingAlgorithm() );

        log.info( "routing style :" + dhtConf.getRoutingStyle() );   
        nodeStatus.put( "style", dhtConf.getRoutingStyle() );     

        log.info( "node id: " + 
                  idap.getID().toBigInteger().toString(16) );
        nodeStatus.put( "ID", 
                        idap.getID().toBigInteger().toString(16) );

        log.info( "messaging address:" + 
                  idap.getAddress().getHostnameOrHostAddress() );        
        nodeStatus.put( "address", 
                        idap.getAddress().getHostnameOrHostAddress() );

        log.info( "messaging port:" + idap.getAddress().getPort() ); 
        nodeStatus.put( "port", 
                        Integer.toString( idap.getAddress().getPort() ) );

        String rtable = dhtRoutingService
            .getRoutingAlgorithm().getRoutingTableHTMLString();

        // replace default URLs with node-status.action pointer
        //-----------------------------------------------------
        
        nodeStatus.put( "routing table", rtable );


        if ( dht.getDHT().getLocalKeys() !=null ){
            log.info( "  local keys: " + dht.getDHT().getLocalKeys() );
            nodeStatus.put( "local keys", 
                            Integer.toString( dht.getDHT().getLocalKeys().size() ) ); 
        } else {
            log.info( "  local keys: 0" );
            nodeStatus.put( "local keys", "0" );
        }

        log.info( "  global keys: " + dht.getDHT().getGlobalKeys().size() ); 
        nodeStatus.put( "global keys", 
                        Integer.toString( dht.getDHT().getGlobalKeys().size() ) );
        

        setMessage(getText(MESSAGE));
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
