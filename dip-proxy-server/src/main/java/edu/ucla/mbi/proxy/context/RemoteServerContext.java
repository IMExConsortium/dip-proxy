package edu.ucla.mbi.proxy.context;

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

import java.util.*;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.proxy.router.*;
import edu.ucla.mbi.proxy.ProxyTransformer;

public class RemoteServerContext {

    private Map<String,Map> services;
    
    private String provider = "";
    private boolean initialized = false;
    
    private int ttl = 0;
    private int timeout = 0;
    private int maxRetry = 1;
    private boolean ramCacheOn = true;
    private boolean dbCacheOn = true;
    private boolean monitorOn = true;
    private boolean remoteProxyOn = true;

    private int debug = 0;
    private Map properties = new HashMap();
    private NativeServer nativeServer = null;
    private RemoteProxyServer proxyServer = null;
    private Router router = null;
    private Set serviceSet = new HashSet();
    private ProxyTransformer transformer = null;

    //*** constructor
    public RemoteServerContext(){};

    public RemoteServerContext ( Map<String, Map> services ) {
        this.services = services;
    }  

    public RemoteServerContext ( Map<String, Map> services, String provider ) {
        this.services = services;
        _init( provider );
    }  

    //*** getter
    public int getTimeout() {
	    return timeout;
    }
    
    public int getTtl(){
        return ttl;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public long getTtlMilli(){
        return 1000L*ttl;
    }

    public boolean isRamCacheOn() {
        return ramCacheOn;
    }
    
    public boolean isDbCacheOn() {
        return dbCacheOn;
    } 
    
    public boolean isMonitorOn() {
        return monitorOn;
    }

    public boolean isRemoteProxyOn() {
        return remoteProxyOn;
    }

    public int getDebugLevel() {
        return debug;
    }

    public String getProvider() {
        return provider;
    }

    public NativeServer getNativeServer() {
        return nativeServer;
    }

    public RemoteProxyServer getProxyProto() {
        return proxyServer;
    }
    
    public Router getRouter() {
        return router;
    }

    public ProxyTransformer getTransformer() {
        return transformer;
    }

    // --------------------------------------
    public void setProperty( String name, Object value ) {
	    properties.put(name,value);
    }
    
    public Object getProperty( String name ) {
	    return properties.get(name);
    }
   
    public Set<String> getServiceSet () {
        return serviceSet;
    }

    //--------------------------------------------------------------------------

    private void _init( String provider ) {
       	Log log = LogFactory.getLog( RemoteServerContext.class );
        
        log.info( "rsc=" + this );
        log.info( "configure(" + provider + ")" );

        this.provider = provider;
	
        Map<String, Object> context = services.get( provider );
        
        timeout = (Integer) context.get( "timeout" );
        log.info( "  timeout=" + timeout );

        ttl = (Integer) context.get( "ttl" );
        log.info( "  ttl=" + ttl );  
 
        maxRetry = Integer.parseInt( (String)context.get( "maxRetry" ) );
        log.info( "  maxRetry=" + maxRetry );
 
        ramCacheOn = (Boolean) context.get( "ramCacheOn" );
        log.info( "  ramCacheOn=" + ramCacheOn );
        
        dbCacheOn = (Boolean) context.get( "dbCacheOn" );
        log.info( "  dbCacheOn=" + dbCacheOn );
        
        remoteProxyOn = (Boolean) context.get( "remoteProxyOn" );
        log.info( "  remoteProxyOn=" + remoteProxyOn );
        
        monitorOn = (Boolean) context.get( "monitorOn" );
        log.info( "  monitorOn=" + monitorOn );
        
        debug = (Integer) context.get( "debug" );
        log.info( "  debug=" + debug );
        
        log.info( "  servers:" );
        
        nativeServer = (NativeServer) context.get( "nativeServer" );
        
        transformer = (ProxyTransformer) context.get( "transformer" );

        serviceSet = (Set) context.get( "serviceSet" );
        
        proxyServer = (RemoteProxyServer) context.get( "proxyProto" );
        log.info( "   proxyProto=" + proxyServer );
        
        router = (Router) context.get( "router" );
        log.info( "   router=" + router );
        
        if( router != null ){
            router.setRemoteServerContext( this );
        }

        initialized=true;
        log.info("configure(" +  provider+ "): DONE");    
    }
}  
