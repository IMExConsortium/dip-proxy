package edu.ucla.mbi.proxy.prolinks;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * ProlinksContext:
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.*;

import java.util.Map;

public class ProlinksContext{ // implements ServiceLifeCycle {

    private static String provider="Prolinks";

    /*
     *  This will be called during the deployement time of the service. 
     */
    
    /*
    public void startUp(ConfigurationContext ignore, AxisService service) {
	Log log = LogFactory.getLog(ProlinksContext.class);
	log.info("ProlinksContext: prolinksPublic Service Starting");
	ServiceContext.addRemoteServer(serverName);

	RemoteServerContext rsc= ServiceContext.getRsc(serverName);

	Map wsc= WSContext.getServiceContext(serverName);
	String refseqService=(String) wsc.get("refseqService");
	rsc.setProperty("refseqService",refseqService);
	log.info("ProlinksContext:  refseqService="+refseqService);
	log.info("ProlinksContext: DONE");    
    }
    
    public void shutDown(ConfigurationContext ctxIgnore, AxisService ignore) {
	Log log = LogFactory.getLog(ProlinksContext.class);
	log.info("ProlinksContext: Dxf Service Stopping");
    }
    */
}  
