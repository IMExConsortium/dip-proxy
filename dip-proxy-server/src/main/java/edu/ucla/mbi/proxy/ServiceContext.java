package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * ServiceContext:
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.HashMap;

public class ServiceContext {
    
    private static HashMap<String,RemoteServerContext> 
	rsc= new HashMap();
    
    public static RemoteServerContext getRsc( String serverName ) {
	return rsc.get( serverName );
    }

    public static void addRemoteServer( String serverName ) {
	
	RemoteServerContext newRsc= new RemoteServerContext();
	newRsc.init( serverName );
	rsc.put( serverName, newRsc );
    }
}  
