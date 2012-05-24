package edu.ucla.mbi.proxy.ncbi;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * NcbiContext:
 *
 *========================================================================= */

//import org.apache.axis2.engine.ServiceLifeCycle;
//import org.apache.axis2.context.ConfigurationContext;
//import org.apache.axis2.description.AxisService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.*;

public class NcbiContext { //implements ServiceLifeCycle {

    private static String provider="NCBI";
    
    /**
     *  This will be called during the deployement time of the service. 
     */

    /*    
    public void startUp(ConfigurationContext ignore, AxisService service) {
	Log log = LogFactory.getLog(NcbiContext.class);
	log.info("NcbiContext: ncbiPubic Service Starting");
	ServiceContext.addRemoteServer(serverName);
	log.info("NcbiContext: DONE");    
    }
    
    public void shutDown(ConfigurationContext ctxIgnore, AxisService ignore) {
	Log log = LogFactory.getLog(NcbiContext.class);
	log.info("NcbiContext: ncbiPublic Service Stopping");
    }
    */
}  
