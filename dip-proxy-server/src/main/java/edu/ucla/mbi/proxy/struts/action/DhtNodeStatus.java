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
import java.io.*;
import java.net.*;

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
    // operations: NOTE add operationAware interface ? 
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
    //---------------------------------------------------------------------

    
    private String update = "false";

    /*
    public void setUpdate( String update ) {
        this.update = update;
    }
    */
    
    private String config;
    public void setConfig ( String config ) {
        this.config = config;
    }

    public String getConfig () {
        this.config = dht.getContext().getDhtContextString();
        return config;
    }

    //---------------------------------------------------------------------

    public String execute() throws Exception {

        log.info("DhtNodeStatus execute");

        log.info( "return configString=" + getConfig() );
        //super.findMenuPage();

        DHTConfiguration dhtConf = dht.getDHT().getConfiguration();  
        RoutingService dhtRoutingService = dht.getDHT().getRoutingService();  
        IDAddressPair idap = dhtRoutingService.getSelfIDAddressPair();

        log.info( "op=" + getOp() );
        log.info( "opp=" + getOpp() );
        
        if(getOp()!=null){
            
            if( getOp().get("view") != null ){            
                return "json";
            }

            if( getOp().get("update") != null ){
                return "update";
            }
            
            if( getOp().get("updateDht") != null 
                && getOpp() != null ) {

                log.info( "opp=" + getOpp() );

                // use new key:value pairs in getOpp() 
                // to update dht.json configuration
                
                for ( String oppKey:getOpp().keySet() ) {
                    String oppVal = (String)getOpp().get( oppKey );
                    log.info( "oppkey=" + oppKey + ", and oppVal=" + oppVal );                 
               
                    dht.getContext().setDhtOption( oppKey, oppVal );
                }

                saveDhtContextToJsonFile();
                //dht.getContext().storeDhtContext( getServletContext() );
                
                dht.reinitialize( true );

                //return "update";
                return "json";
            }
        }

       
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
        //                idap.getAddress().getHostnameOrHostAddress() );
                          idap.getAddress().getHostAddress() );
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


        if( update.equalsIgnoreCase("true")){
            dht.reinitialize( true );
        }

        return SUCCESS;
    }

    private void saveDhtContextToJsonFile() throws Exception {
        log.info( "storeDhtContext: stotingContext... " );

        String jsonConfigFile = (String) dht.getContext().getJsonContext()
            .getConfig().get( "json-config" );

        log.info( "saveDhtContextToJsonFile: jsonConfigFile=" + jsonConfigFile );

        String srcPath = getServletContext().getRealPath( jsonConfigFile );
        log.info( " srcPath=" + srcPath );

        File sf = new File( srcPath );

        try {
            PrintWriter spw = new PrintWriter( sf );
            dht.getContext().getJsonContext().writeJsonConfigDef( spw );
            spw.close();
        } catch ( Exception ex ) {
            throw ex;
        }

        log.info( "saveDhtContextToJsonFile: after writing to json file. " );

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
