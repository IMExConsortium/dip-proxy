package edu.ucla.mbi.server;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * RemoteServerContext:
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.HashMap;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.proxy.router.*;
import edu.ucla.mbi.proxy.ProxyTransformer;

public class RemoteServerContext {
    
    private String provider="";
    private boolean initialized=false;
    
    private int ttl=0;
    private int timeout=0;
    private boolean cacheOn = true;
    private boolean monitorOn = true;
    private boolean remoteProxyOn = true;

    private int debug = 0;

    private ProxyTransformer transformer= null;
    
    private Map properties = new HashMap();

    private RemoteServer nativeServer = null;
    private RemoteProxyServer proxyServer = null;
    
    private Router router = null;
    
    public int getTimeout(){
	    return timeout;
    }
    
    public int getTtl(){
        return ttl;
    }

    public long getTtlMilli(){
        return 1000L*60*60*24*ttl;
    }
    
    public boolean isCacheOn(){
        return cacheOn;
    }
    
    public boolean isMonitorOn(){
        return monitorOn;
    }

    public boolean isRemoteProxyOn(){
        return remoteProxyOn;
    }

    public int getDebugLevel() {
        return debug;
    }

    public String getProvider(){
        return provider;
    }

    public RemoteServer getNativeServer(){
        return nativeServer;
    }

    public RemoteProxyServer getProxyProto(){
        return proxyServer;
    }
    
    public Router createRouter(){
      	return router.createRouter();
    }

    public ProxyTransformer getTransformer(){
	    return transformer;
    }

    public void setProperty(String name, Object value){
	    properties.put(name,value);
    }
    
    public Object getProperty(String name){
	    return properties.get(name);
    }
    
    public void init( String provider ) {
       	Log log = LogFactory.getLog( RemoteServerContext.class );

	    log.info( "rsc=" + this );
        log.info( "configure(" + provider + ")" );

	    this.provider = provider;
	
	    Map context = WSContext.getService( provider );
                                                                           	
	    log.info( "  timeout=" + context.get( "timeout" ) );
	    timeout = (Integer) context.get( "timeout" );
	
	    log.info( "  ttl=" + context.get( "ttl" ) );
	    ttl = (Integer) context.get( "ttl" );

	    log.info( "  cacheOn=" + context.get( "cacheOn" ) );
	    cacheOn = (Boolean) context.get( "cacheOn" );
                
	    log.info( "  remoteProxyOn=" + context.get( "remoteProxyOn" ) );
	    remoteProxyOn = (Boolean) context.get( "remoteProxyOn" );
        
	    log.info( "  monitorOn=" + context.get( "monitorOn" ) );
	    monitorOn = (Boolean) context.get( "monitorOn" );

	    log.info( "  debug=" + context.get( "debug" ) );
	    debug = (Integer) context.get( "debug" );
        
	    log.info( "  transformer=" + context.get( "transformer" ) );
	    transformer = (ProxyTransformer) context.get( "transformer" );
        
        log.info( "  servers:" );

        log.info( "   nativeServer=" + context.get( "nativeServer" ) );
        nativeServer = (RemoteServer) context.get( "nativeServer" );

        
        log.info( "   proxyProto=" + context.get( "proxyProto" ) );
        proxyServer = (RemoteProxyServer) context.get( "proxyProto" );
        
	    log.info( "   router=" + context.get( "router" ) );
	    router = (Router) context.get( "router" );
	    router.setRemoteServerContext( this );
        
	    initialized=true;
	    log.info("configure(" +  provider+ "): DONE");    
    }

    public void initOldVersion( String provider ) {
       	Log log = LogFactory.getLog( RemoteServerContext.class );

	    log.info( "rsc=" + this );
        log.info( "configure(" + provider + ")" );

	    this.provider = provider;
	
	    Map context = WSContext.getOldVersionService( provider );
                                                                           	
	    log.info( "  timeout=" + context.get( "timeout" ) );
	    timeout = (Integer) context.get( "timeout" );
	
	    log.info( "  ttl=" + context.get( "ttl" ) );
	    ttl = (Integer) context.get( "ttl" );

	    log.info( "  cacheOn=" + context.get( "cacheOn" ) );
	    cacheOn = (Boolean) context.get( "cacheOn" );
                
	    log.info( "  remoteProxyOn=" + context.get( "remoteProxyOn" ) );
	    remoteProxyOn = (Boolean) context.get( "remoteProxyOn" );
        
	    log.info( "  monitorOn=" + context.get( "monitorOn" ) );
	    monitorOn = (Boolean) context.get( "monitorOn" );

	    log.info( "  debug=" + context.get( "debug" ) );
	    debug = (Integer) context.get( "debug" );
        
	    log.info( "  transformer=" + context.get( "transformer" ) );
	    transformer = (ProxyTransformer) context.get( "transformer" );
        
        log.info( "  servers:" );

        log.info( "   nativeServer=" + context.get( "nativeServer" ) );
        nativeServer = (RemoteServer) context.get( "nativeServer" );

        
        log.info( "   proxyProto=" + context.get( "proxyProto" ) );
        proxyServer = (RemoteProxyServer) context.get( "proxyProto" );
        
	    log.info( "   router=" + context.get( "router" ) );
	    router = (Router) context.get( "router" );
	    router.setRemoteServerContext( this );
        
	    initialized=true;
	    log.info("configure(" +  provider+ "): DONE");    
    }
    
}  
