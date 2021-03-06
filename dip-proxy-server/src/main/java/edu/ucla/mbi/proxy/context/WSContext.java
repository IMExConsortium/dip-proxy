package edu.ucla.mbi.proxy.context;
                                                                           
/*==============================================================================
 *                                                                             $
 * WSContext - global configuration of the dip-proxy server                    $ 
 *                                                                             $
 *=========================================================================== */

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.monitor.Scheduler;
import edu.ucla.mbi.cache.orm.*;
import edu.ucla.mbi.util.cache.*;
import edu.ucla.mbi.util.struts.action.*;

public class WSContext{
    
    private static final int DEFAULT_TTL     = 14;   // two weeks
    private static final int DEFAULT_TIMEOUT = 300;  // 300s
    private static final int DEFAULT_DEBUG = 0;       
    
    private Map<String,Map> services;

    private Map<String, RemoteServerContext> serverContexts 
        = new HashMap<String, RemoteServerContext>();

    private McClient mcClient;
    private DipProxyDAO dipProxyDAO;

    private Map<String,Object> state  =
        new HashMap<String,Object>();

    private int counter = 0;
    
    public void threadCountUp(){

        synchronized( state ){
            if( state.get( "thread-count" ) == null ){
                state.put( "thread-count", 0 );
            } else {
                state.put( "thread-count", 
                           (Integer)state.get("thread-count") + 1 );
            }
        }
    }

    public void threadCountDown(){
        synchronized( state ){
            if( state.get( "thread-count" ) == null ){
                state.put( "thread-count", 0 );
            } else {
                if( state.get( "thread-count" ) != null ) {
                    int cnt = (Integer) state.get( "thread-count" );
                    if( cnt > 0 ){
                        state.put( "thread-count", cnt - 1 );
                    }
                }            
            }
        }
    }

    public int getThreadCount(){
        if( state.get( "thread-count" ) == null ) {
            return 0;
        }
        return (Integer)state.get( "thread-count" );        
    }
   
    public void setDipProxyDAO( DipProxyDAO dao ){
        this.dipProxyDAO = dao;
    }
 
    public void setServices( Map<String,Map> services ) {
        this.services = services;
    }

    public void setMcClient ( McClient client ) {
        this.mcClient = client;
    }
   
    public DipProxyDAO getDipProxyDAO(){
        return dipProxyDAO;
    }

    public Map<String,Map> getServices() {
	    return services;
    }

    public Set<String> getProviderSet(){
        return services.keySet();
    }
  
    public Map getProvider( String provider ) {
        return (Map) services.get( provider );
    }

    public McClient getMcClient() {
        return mcClient;
    }

    public ProxyDxfTransformer getDxfTransformer ( String provider) {
        return new ProxyDxfTransformer( this, provider );
    }

    public boolean isRamCacheOn( String provider ) {
        return this.getServerContext( provider ).isRamCacheOn();
    }

    public boolean isDbCacheOn( String provider ) {
        return this.getServerContext( provider ).isDbCacheOn();
    }

    public boolean isRemoteProxyOn( String provider ) {
        return this.getServerContext( provider ).isRemoteProxyOn();
    }

    public int getTtl( String provider ) {
        return this.getServerContext( provider ).getTtl();
    }

    public NativeServer getNativeServer( String provider ) {
        return this.getServerContext( provider ).getNativeServer();
    }

    public int getTimeout( String provider ) {
        return this.getServerContext( provider ).getTimeout();
    }
 
    public ProxyTransformer getTransformer( String provider ) {
        return this.getServerContext( provider ).getTransformer();
    }

    public Set<String> getServiceSet ( String provider ) {
        return this.getServerContext( provider.toUpperCase() ).getServiceSet();
    }

    public int getMaxRetry( String provider ) {
        return this.getServerContext( provider ).getMaxRetry();
    }

    //---------------------------------------------------------------------

    private RemoteServerContext getServerContext( String provider ) {
 
	    Log log = LogFactory.getLog( WSContext.class );
	    log.info( "ProxyWS: WSContext.getServerContext(" + provider + ")" );

	    if( serverContexts.get( provider ) == null ) {
	        RemoteServerContext rsc = 
                    new RemoteServerContext( services, provider );
                serverContexts.put( provider, rsc );
	    }
	    return serverContexts.get( provider );
    }

    public void initialize() {
        Log log = LogFactory.getLog( WSContext.class );
        log.info( "ProxyWS: WSContext initializing..." );
        log.info( "ProxyWS: Known remote services:" );
        log.info( "ProxyWS: mcClient=" + mcClient );

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
                    }
                }
            }
	        
            ( (Map) services.get( service ) ).put( "ttl", intTtl );
                
            // Remote Service Timeout
            //-----------------------
                
            String timeout =
                (String) ( (Map) services.get( service ) ).get( "timeout" );
	    
            int intTimeout = DEFAULT_TIMEOUT * 1000 ; // convert to millis
                
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
                        
                } 
            }
                
            ( (Map) services.get( service ) ).put( "timeout", intTimeout );
                
            log.info( "ProxyWS:  " + service + " (ttl: " + ttl + " days; " +
                      "timeout=" + timeout + " s)" );
            
            // ramCache flag
            //------------
                
            String ramCacheOn =
                (String) ( (Map) services.get( service ) ).get( "ramCache" );
            boolean isRamCacheOn = true;
            
            if ( ramCacheOn != null ) {
                if ( ramCacheOn.equalsIgnoreCase( "true" ) 
                     || ramCacheOn.equalsIgnoreCase( "on" ) 
                     || ramCacheOn.equalsIgnoreCase( "yes" ) ) {
                        
                    isRamCacheOn = true;
                        
                } else if ( ramCacheOn.equalsIgnoreCase( "false" ) 
                            || ramCacheOn.equalsIgnoreCase( "off" ) 
                            || ramCacheOn.equalsIgnoreCase( "no" ) ) {
                        
                    isRamCacheOn = false;
                        
                } else {
                    throw new ProxyException( "Service: " + service +
                                              " ramCache flag format error");
                }
                log.info( "ProxyWS: ramCache=" + isRamCacheOn );
            } else {
                log.info( "ProxyWS: ramCache=" + isRamCacheOn +
                          " (default)");
            }

            ( (Map) services.get( service ) ).put( "ramCacheOn", isRamCacheOn );

            // dbCache flag
            //------------
                
            String dbCacheOn =
                (String) ( (Map) services.get( service ) ).get( "dbCache" );
            boolean isDbCacheOn = true;
                
            if ( dbCacheOn != null ) {
                if ( dbCacheOn.equalsIgnoreCase( "true" ) 
                     || dbCacheOn.equalsIgnoreCase( "on" ) 
                     || dbCacheOn.equalsIgnoreCase( "yes" ) ) {
                        
                    isDbCacheOn = true;
                        
                } else if ( dbCacheOn.equalsIgnoreCase( "false" ) 
                            || dbCacheOn.equalsIgnoreCase( "off" ) 
                            || dbCacheOn.equalsIgnoreCase( "no" ) ) {
                        
                    isDbCacheOn = false;
                        
                } else {
                    throw new ProxyException( "Service: " + service +
                                              " dbCache flag format error" );
                }
                log.info( "ProxyWS:   dbCache=" + isDbCacheOn );
            } else {
                log.info( "ProxyWS:   dbCache=" + isDbCacheOn +
                          " (default)");
            }
                
            ( (Map) services.get( service ) ).put( "dbCacheOn", isDbCacheOn );


            // Monitor flag
            //-------------
                
            String monitorOn =
                (String) ( (Map) services.get( service ) ).get( "monitor" );
            boolean isMonitorOn = false;
                
            if ( monitorOn != null ) {
                if ( monitorOn.equalsIgnoreCase( "true" ) 
                     || monitorOn.equalsIgnoreCase( "on" ) 
                     || monitorOn.equalsIgnoreCase( "yes" ) ) {
                        
                    isMonitorOn = true;

                } else if (monitorOn.equalsIgnoreCase( "false" ) 
                           || monitorOn.equalsIgnoreCase( "off" ) 
                           || monitorOn.equalsIgnoreCase( "no" ) ) {
                        
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
                if ( remoteProxyOn.equalsIgnoreCase( "true" ) 
                     || remoteProxyOn.equalsIgnoreCase( "on" ) 
                     || remoteProxyOn.equalsIgnoreCase( "yes" ) ) {
                        
                    isRemoteProxyOn = true;
                    
                } else if ( remoteProxyOn.equalsIgnoreCase( "false" ) 
                            || remoteProxyOn.equalsIgnoreCase( "off" ) 
                            || remoteProxyOn.equalsIgnoreCase( "no" ) ) {
                        
                    isRemoteProxyOn = false;
                        
                } else {
                    throw new ProxyException( "Provider: " + service +
                                              " remote proxy flag format error" );
                }
                log.info( "ProxyWS:   remote proxy=" + isRemoteProxyOn );
            } else {
                log.info( "ProxyWS:   monitor=" + isRemoteProxyOn +
                          " (default)");
            }
                
            ( (Map) services.get( service ) ).put( "remoteProxyOn", isRemoteProxyOn );
                
            // proxy prototype
            //----------------
                
            NativeServer proxyProto =         
                (NativeServer) ( (Map) services.get( service ) ).get( "proxyProto" );
            ( (Map) services.get( service ) ).put( "proxyProto", proxyProto );

            
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
