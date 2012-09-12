package edu.ucla.mbi.proxy.router;

/*===========================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/service#$
 * $Id:: CachingService.java 130 2009-02-03 17:58:49Z wyu                   $
 * Version: $Rev:: 130                                                      $
 *===========================================================================
 *
 * SimpleRouter:
 *   selects remote server to call by iterating through the proxy list
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

import edu.ucla.mbi.server.RemoteServerContext;
import edu.ucla.mbi.proxy.RemoteServer;

public class SimpleRouter implements Router {
    
    private RemoteServerContext rsc = null;
    private int maxRetry = 0;
    
    private int currentServer = -1;

    public  SimpleRouter() { }
    
    public  SimpleRouter( RemoteServerContext rsc ){ 
        this.rsc = rsc;
    }

    public Router createRouter(){
        return new SimpleRouter( this.rsc );
    }

    public void setRemoteServerContext( RemoteServerContext rsc ){
        this.rsc = rsc;
    }
    
    public RemoteServerContext getRemoteServerContext(){
        return rsc;
    }

    public void setMaxRetry( int retry ) {
        this.maxRetry = retry;
    }
    
    public int getMaxRetry() {
        return maxRetry;
    }

    public RemoteServer getNativeServer( String service ){
        
        Log log = LogFactory.getLog(SimpleRouter.class);
        return rsc.getNativeServerMap().get( service );
    } 
        
    
    public RemoteServer getLastProxyServer( String service ) {
        
        /*
	    Log log = LogFactory.getLog(SimpleRouter.class);
        List<RemoteProxyServer> servers = rsc.getProxyServers();
	
        RemoteServer server = servers.get( currentServer );
        log.info("server # "+currentServer);
	    */
        
	    return null;
    }
    
    public RemoteServer getNextProxyServer( String service ) {
	
        /*
	    Log log = LogFactory.getLog(SimpleRouter.class);
 
        List<RemoteProxyServer> servers = rsc.getProxyServers();
	
	    // rotate servers
	    //---------------
        
	    currentServer++;
	
	    if ( currentServer == servers.size() || 
	        currentServer < 0 ) {
	        currentServer = 0;
	    }
	
        log.info("server # "+currentServer);
        RemoteServer server = servers.get( currentServer );
        */

        return null;
    }
    
    public RemoteServer getNextProxyServer( String namespace,
                                            String accession,
                                            String operation ){
        
        return this.getNextProxyServer( operation );
    }

    // observer intereface
    //--------------------

    public void update( Observable o, Object arg ) {

    }
            
}
