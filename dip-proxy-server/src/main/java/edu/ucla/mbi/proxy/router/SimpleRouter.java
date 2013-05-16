package edu.ucla.mbi.proxy.router;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
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

import edu.ucla.mbi.proxy.context.RemoteServerContext;
import edu.ucla.mbi.proxy.NativeServer;
import edu.ucla.mbi.proxy.RemoteProxyServer;

public class SimpleRouter implements Router {
    
    private RemoteServerContext rsc = null;

    private static int currentProxyServer = -1;
    
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
    
    private Map<String, Object> config = new HashMap<String, Object>();

    public void setConfig( Map<String, Object> config ){
        this.config= config;
    }

       
    // observer intereface
    //--------------------

    public void update( Object observer, Object arg ) {
        // no updates needed
    }
    
    public NativeServer getNextProxyServer( String provider,
                                            String service,
                                            String namespace,
                                            String accession ) {

        Log log = LogFactory.getLog(SimpleRouter.class);        
        log.info( "config=" + config );

        List<String> pul = new ArrayList<String>();
        pul = (List<String>) config.get("proxy-url-list");
        

        log.info( "pul=" + pul );
        log.info( "currentProxyServer=" + currentProxyServer );
              
        if( currentProxyServer + 1 == pul.size() ) {
            currentProxyServer = -1;
        }
         
        String url = (String) pul.get( ++currentProxyServer );
                 
        return new RemoteProxyServer( url );
    }
}
