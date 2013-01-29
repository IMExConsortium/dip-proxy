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

    private boolean ramCacheOn = true;
    private boolean dbCacheOn = true;
    private boolean monitorOn = true;
    private boolean remoteProxyOn = true;

    private int debug = 0;

    private Map properties = new HashMap();

    private Map<String, RemoteServer> nativeServerMap = new HashMap();

    private RemoteProxyServer proxyServer = null;

    private Router router = null;
    
    //*** getter
    public int getTimeout(){
	    return timeout;
    }
    
    public int getTtl(){
        return ttl;
    }

    public long getTtlMilli(){
        return 1000L*60*60*24*ttl;
    }

    public boolean isRamCacheOn() {
        return ramCacheOn;
    }
    
    public boolean isDbCacheOn(){
        return dbCacheOn;
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

    public Map<String, RemoteServer> getNativeServerMap(){
        return nativeServerMap;
    }

    public RemoteProxyServer getProxyProto(){
        return proxyServer;
    }
     
    public Router createRouter(){
      	return router.createRouter();
    }

    public void setProperty(String name, Object value){
	    properties.put(name,value);
    }
    
    public Object getProperty(String name){
	    return properties.get(name);
    }
   
    public void setNativeServerMap( Map<String, RemoteServer> nativeMap ) {
        this.nativeServerMap = nativeMap;
    } 


    public void init( String provider ) {
       	Log log = LogFactory.getLog( RemoteServerContext.class );

	    log.info( "rsc=" + this );
        log.info( "configure(" + provider + ")" );

	    this.provider = provider;
	
	    Map context = WSContext.getProvider( provider );
                                                                           	
	    log.info( "  timeout=" + context.get( "timeout" ) );
	    timeout = (Integer) context.get( "timeout" );
	
	    log.info( "  ttl=" + context.get( "ttl" ) );
	    ttl = (Integer) context.get( "ttl" );

	    log.info( "  ramCacheOn=" + context.get( "ramCacheOn" ) );
	    ramCacheOn = (Boolean) context.get( "ramCacheOn" );

        log.info( "  dbCacheOn=" + context.get( "dbCacheOn" ) );
        dbCacheOn = (Boolean) context.get( "dbCacheOn" );
                
	    log.info( "  remoteProxyOn=" + context.get( "remoteProxyOn" ) );
	    remoteProxyOn = (Boolean) context.get( "remoteProxyOn" );
        
	    log.info( "  monitorOn=" + context.get( "monitorOn" ) );
	    monitorOn = (Boolean) context.get( "monitorOn" );

	    log.info( "  debug=" + context.get( "debug" ) );
	    debug = (Integer) context.get( "debug" );
       
        log.info( "  servers:" );

        nativeServerMap = (Map<String, RemoteServer>) context.get( "nativeServer" );
        
        log.info( "   proxyProto=" + context.get( "proxyProto" ) );
        proxyServer = (RemoteProxyServer) context.get( "proxyProto" );
        
	    log.info( "   router=" + context.get( "router" ) );
	    router = (Router) context.get( "router" );
	    router.setRemoteServerContext( this );
        
	    initialized=true;
	    log.info("configure(" +  provider+ "): DONE");    
    }
}  
