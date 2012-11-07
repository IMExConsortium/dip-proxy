package edu.ucla.mbi.server;
                                                                           
/*==============================================================================
 * $HeadURL::                                                                  $ 
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * WSContext - global configuration of the dip-proxy server
 *
 *==============================================================================
 */

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.proxy.router.*;
import edu.ucla.mbi.monitor.Scheduler;
import edu.ucla.mbi.cache.orm.*;

import edu.ucla.mbi.util.struts2.action.*;

public class WSContext{
    
    private static final int DEFAULT_TTL     = 14;     // two weeks
    private static final int DEFAULT_TIMEOUT = 30000;  // 30s
    private static final int DEFAULT_DEBUG = 0;        // 
    
    private static Map<String,Map> services;

    private static Map<String, RemoteServerContext> serverContexts 
                                = new HashMap<String, RemoteServerContext>();

    private static Dht proxyDht;

    private static int port = 80;

    private static Scheduler scheduler;
 
    private static long waitMillis = 5000;
    
    private static int threadRunMinutes = 10; // 10 minutes

    private static ProxyTransformer transformer;

    //*** setter
    public void setPort( int port ) {
        this.port = port;
    }
    
    public void setTransformer ( ProxyTransformer tf ) {
        this.transformer = tf;
    }

    public void setDht( Dht dht ) {
        proxyDht = dht;
    }

    public void setScheduler( Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void setWaitMillis( long time ){
        waitMillis = time;
    }

    public void setThreadRunMinutes ( int time ) {
        threadRunMinutes = time;
    }

    public void setServices( Map<String,Map> services ) {
        this.services = services;
    }

    //*** getter
    public static int getPort() {
        return port;
    }

    public static ProxyTransformer getTransformer () {
        return transformer;
    }

    public static Dht getDht() {
        return proxyDht;
    }

    public Map<String,Map> getServices() {
	    return services;
    }
    
    public static Map getProvider( String provider ) {
        return (Map) services.get( provider );
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static long getWaitMillis(){
        return waitMillis;
    }

    public static int getThreadRunMinutes() {
        return threadRunMinutes;
    }

    //---------------------------------------------------------------------

    public static RemoteServerContext getServerContext( String provider ) {
	    Log log = LogFactory.getLog( WSContext.class );
	    log.info( "ProxyWS: WSContext.getServerContext(" + provider + ")" );

	    if( serverContexts.get( provider ) == null ) {
	        RemoteServerContext rsc = new RemoteServerContext();
	        rsc.init( provider );
	        serverContexts.put( provider, rsc );
	    }
	    return serverContexts.get( provider );
    }

    public void initialize() {
        Log log = LogFactory.getLog( WSContext.class );
        log.info( "ProxyWS: WSContext initializing..." );
        log.info( "ProxyWS: Known remote services:" );

	    for( Iterator<String> 
		        i = services.keySet().iterator(); i.hasNext(); ) {

	        String service = i.next();
	    
	        // Time To Live
	        //-------------
	    
	        String ttl = 
		        (String) ( (Map) services.get( service ) ).get( "ttl" );
	        int intTtl = DEFAULT_TTL*60*60*24;
	        if ( ttl != null ) {
		        if (ttl.replaceAll( "\\s+", "" ).matches( "^\\d+$" ) ) {
		            try {
			            // detault units: days
			            intTtl = Integer.parseInt( ttl );
			            // convert to seconds
			            intTtl = intTtl*60*60*24;
		            } catch ( NumberFormatException nfe ) {
			            log.info( "ProxyWS: ttl=" + ttl +
				                  " :format error. Using default." );
                        throw nfe;
                                                  
		            }
		        } else {
		            log.info( "ProxyWS: ttl="+ttl+
				              " :unknown units/format. Using default.");
		            ttl = String.valueOf( DEFAULT_TTL );
		        }
	        } else {
		        log.info( "ProxyWS: ttl not specified: Using default." );
		        ttl = String.valueOf( DEFAULT_TTL );
	        }
	         
            ( (Map) services.get( service ) ).put( "ttl", intTtl );
	    

	        // Remote Service Timeout
	        //-----------------------

	        String timeout =
		        (String) ( (Map) services.get( service ) ).get( "timeout" );
	    
	        int intTimeout = DEFAULT_TIMEOUT;
	        if( timeout != null ) {
                if ( timeout.replaceAll( "\\s+", "" ).matches( "^\\d+$" ) ) {
		            try {
                        // detault units: seconds
			            intTimeout = Integer.parseInt( timeout );
			            // convert to miliseconds
                        intTimeout = intTimeout*1000; 
                    } catch( NumberFormatException nfe ) {
                        log.info( "ProxyWS: timeout=" + timeout +
				                  " :format error. Using default." );
                    }
                } else {
                    log.info("ProxyWS: timeout="+timeout+
                             " :unknown units/format. Using default.");
		            timeout=String.valueOf(DEFAULT_TIMEOUT);
                }
            } else {
                log.info( "ProxyWS: ttl not specified: Using default." );
		        timeout=String.valueOf( DEFAULT_TIMEOUT );
            }
	    
	        ( (Map) services.get( service ) ).put( "timeout", intTimeout );
	    
	        log.info( "ProxyWS:  " + service + " (ttl: " + ttl + " days; " +
		              "timeout=" + timeout + " s)" );
	    

	        // Cache flag
            //------------

            String cacheOn =
                (String) ( (Map) services.get( service ) ).get( "cache" );
            boolean isCacheOn = true;
	    
            if ( cacheOn != null ) {
                if ( cacheOn.equalsIgnoreCase( "true" ) ||
                     cacheOn.equalsIgnoreCase( "on" ) ||
                     cacheOn.equalsIgnoreCase( "yes" )){
                    isCacheOn = true;
                } else if (cacheOn.equalsIgnoreCase( "false" ) ||
                           cacheOn.equalsIgnoreCase( "off" ) ||
                           cacheOn.equalsIgnoreCase( "no" ) ){
                    isCacheOn = false;
                } else {
                    throw new ProxyException( "Service: " + service +
                                              " cache flag format error");
                }
                log.info( "ProxyWS:   cache=" + isCacheOn );
            } else {
                log.info( "ProxyWS:   cache=" + isCacheOn +
                          " (default)");
            }

            ( (Map) services.get( service ) ).put( "cacheOn", isCacheOn );

            // Monitor flag
            //-------------

            String monitorOn =
                (String) ( (Map) services.get( service ) ).get( "monitor" );
            boolean isMonitorOn = false;

            if ( monitorOn != null ) {
                if ( monitorOn.equalsIgnoreCase( "true" ) ||
                     monitorOn.equalsIgnoreCase( "on" ) ||
                     monitorOn.equalsIgnoreCase( "yes" ) ){
                    isMonitorOn = true;
                } else if (monitorOn.equalsIgnoreCase( "false" ) ||
                           monitorOn.equalsIgnoreCase( "off" ) ||
                           monitorOn.equalsIgnoreCase( "no" ) ){
                    isMonitorOn = false;
                } else {
                    throw new ProxyException( "Service: " + service +
                                              " monitor flag format error");
                }
                log.info( "ProxyWS:   monitor=" + isMonitorOn );
            } else {
                log.info( "ProxyWS:   monitor=" + isMonitorOn +
                          " (default)");
            }

            ( (Map) services.get( service ) ).put( "monitorOn", isMonitorOn );
	    
	    
	        // Monitor Interval
	        //-----------------
 

	        // Monitor query
	        //--------------

            // Remote Proxy flag
            //------------------

            String remoteProxyOn =
                (String) ( (Map) services.get( service ) ).get( "remoteProxy" );

            boolean isRemoteProxyOn = false;

            if ( remoteProxyOn != null ) {
                if ( remoteProxyOn.equalsIgnoreCase( "true" ) ||
                     remoteProxyOn.equalsIgnoreCase( "on" ) ||
                     remoteProxyOn.equalsIgnoreCase( "yes" ) )
                {
                    isRemoteProxyOn = true;
                } else if (remoteProxyOn.equalsIgnoreCase( "false" ) ||
                           remoteProxyOn.equalsIgnoreCase( "off" ) ||
                           remoteProxyOn.equalsIgnoreCase( "no" ) )
                {
                    isRemoteProxyOn = false;
                } else {
                    throw new ProxyException( "Provider: " + service +
                                              " remote proxy flag format error");
                }
                log.info( "ProxyWS:   remote proxy=" + isRemoteProxyOn );
            } else {
                log.info( "ProxyWS:   monitor=" + isRemoteProxyOn +
                          " (default)");
            }

            ( (Map) services.get( service ) ).put( "remoteProxyOn", isRemoteProxyOn );
            
	        // proxy prototype
	        //----------------

            //RemoteProxyServer proxyProto = 
            //    (RemoteProxyServer) ( (Map) services.get( service ) ).get( "proxyProto" );
            
            RemoteServer proxyProto =         
                (RemoteServer) ( (Map) services.get( service ) ).get( "proxyProto" );
            ( (Map) services.get( service ) ).put( "proxyProto", proxyProto );

	        // router
	        //-------

            Router router = 
                (Router) ( (Map) services.get( service ) ).get( "router" );
            
            ( (Map) services.get( service ) ).put( "router", router );
            
	        // debug level
	        //------------
	    
	        String debug = 
		        (String) ( (Map) services.get( service ) ).get( "debug" );
	        int intDebug = DEFAULT_DEBUG;
	        if ( debug != null ) {
		        if (debug.replaceAll( "\\s+", "" ).matches( "^\\d+$" ) ) {
		            try {
			            // detault units: days
			            intDebug = Integer.parseInt( debug );
		            } catch ( NumberFormatException nfe ) {
			            log.info( "ProxyWS: debug=" + debug +
				                  " :format error. Using default." );
                    }
                } else {
                    log.info("ProxyWS: debug="+ debug +
                             " :unknown units/format. Using default.");
                }
	        } else {
		        log.info( "ProxyWS: debug level not specified: Using default." );
            }
	         
            ( (Map) services.get( service ) ).put( "debug", intDebug );

            //*** initialize RemoteServerContext for individual provider
            RemoteServerContext rsc = getServerContext( service );

	    }

	    log.info( "ProxyWS: WSContext initializing... DONE" );
    }

    public void cleanup() {

        Log log = LogFactory.getLog( WSContext.class );
        log.info( "cleanup called" );
    }

    public static String info() {
	    return "WSContext: info";
    }
}
