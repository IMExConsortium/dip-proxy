package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ConfigurablePortImpl - super class used to set WSContext 
 *                                  
 *=========================================================================== */

import edu.ucla.mbi.server.WSContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigurablePortImpl {
    
    protected WSContext context;   

    public void setContext ( WSContext context ) {
        this.context = context;
    }     

    public void initialize() {

        Log log = LogFactory.getLog( this.getClass() );
        log.info( "StrutsPortImpl initializing..." );
    } 
}
