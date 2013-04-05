package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * StrutsPortImpl - super class used to set WSContext 
 *                                  
 *=========================================================================== */

import edu.ucla.mbi.server.WSContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class StrutsPortImpl {
    
    protected WSContext context;   

    public void setContext ( WSContext context ) {
        this.context = context;
    }     

    public void initialize() {

        Log log = LogFactory.getLog( StrutsPortImpl.class );
        log.info( "StrutsPortImpl initializing..." );
    } 
}
