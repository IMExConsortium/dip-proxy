package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ConfigurableServer - super class used to set WSContext 
 *                                  
 *=========================================================================== */

import edu.ucla.mbi.server.WSContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigurableServer {
    
    protected WSContext wsContext;   

    public void setWsContext ( WSContext context ) {
        this.wsContext = context;
    }     

    public void initialize() {

        Log log = LogFactory.getLog( this.getClass() );
        log.info( "ConfigurablePortImpl initializing..." );
    } 
}
