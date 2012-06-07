package edu.ucla.mbi.proxy.router;

/*===========================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/service#$
 * $Id:: CachingService.java 130 2009-02-03 17:58:49Z wyu                   $
 * Version: $Rev:: 130                                                      $
 *===========================================================================
 *
 * DhtRouter:
 *   selects remote server to call 
 *
 *========================================================================= */
 
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ow.id.*;
import ow.dht.*;
import ow.messaging.*;
import ow.routing.*;

import java.net.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;

public class DhtRouter implements Router {
    
    private RemoteServerContext rsc = null;
    private Dht proxyDht = null;
    private int maxRetry = 0;
    
    private RemoteServer currentServer = null;
    
    private DhtRouter() { }
    
    private DhtRouter( RemoteServerContext rsc,
                       Dht dht, int maxRetry ) { 
	    this.rsc = rsc;
        this.proxyDht = dht;
        this.maxRetry = maxRetry;
    }

    public Router createRouter(){
        return new DhtRouter( this.rsc, this.proxyDht, this.maxRetry );
    }
    
    public void setDht( Dht dht ){
        this.proxyDht = dht;
    }
    
    public void setMaxRetry( int retry ) {
        this.maxRetry= retry;
    }

    public int getMaxRetry() {
        return maxRetry;
    }

    public void setRemoteServerContext( RemoteServerContext rsc ){
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info(" setRemoteServerContext=" + rsc );
        this.rsc = rsc;
    }

    public RemoteServerContext getRemoteServerContext(){
        return rsc;
    }
 
    public RemoteServer getNativeServer(){
        
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info("getNativeServer(native server= " + 
                 rsc.getNativeServer() + ")" );
        return rsc.getNativeServer();
    }
    
    public ID getRecordID( String provider,
                           String service,
                           String namespace,
                           String accession ){
        
        String recordStrId = provider + ":" + service +
            namespace + ":" + accession + ":";
        
        return ID.getSHA1BasedID( recordStrId.getBytes() );
    }
    
    public ID getRecordID( String service, 
                           String namespace,
                           String accession ){
        
        String recordStrId = rsc.getProvider() + ":" + service +
            namespace + ":" + accession + ":";
        
        return ID.getSHA1BasedID( recordStrId.getBytes() );
    }
    
    public RemoteServer getLastProxyServer() {
        
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info("getLastProxyServer (last proxy server= " + 
                 currentServer + ")" );

        if (currentServer != null  ) {
            return currentServer;
        } 
        log.info("  falling back to native" );
        return this.getNativeServer();
    }

    public RemoteServer getNextProxyServer() {
        
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info( "getNextProxyServer() (provider=" + 
                  rsc.getProvider() + ")" );
        return currentServer       ;
        
    }
    
    public RemoteServer getNextProxyServer( String service, 
                                            String namespace,
                                            String accession ) {
        
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info( "getNextProxyServer(args) (provider=" + 
                  rsc.getProvider() + ")" );
        log.info( " SRV=" + service + 
                  " NS=" + namespace + " AC=" + accession );
        
        ID rid = this.getRecordID( service, namespace, accession );
        
        return this.getNextProxyServer( rid );
    }
    
    public RemoteServer getNextProxyServer( ID rid ) {
        
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info( "  rid=" + rid.toString(16) + " @ " + proxyDht );        
        RemoteProxyServer pserver = null;
        RemoteServer remote = null;
        
        String lastAddress = proxyDht.getLastAddress( rid );
        
        if ( lastAddress != null && 
             ! ( lastAddress.equals(rsc.getProxyProto().getAddress() ) ) 
             ) {
            
            RemoteProxyServer remoteProxy =
                rsc.getProxyProto().getRemoteProxyServerInstance();
            remoteProxy.setAddress( lastAddress );
            remote = remoteProxy;
            log.info( "   remote URL=" + lastAddress );
            //if ( rsc.getDebugLevel() == 1 ){
            //    remote = rsc.getNativeServer();
            //}
        } else {
            remote = rsc.getNativeServer();
            log.info( "   remote==native " + remote );
        }
        
        currentServer = remote;
        return remote;
    }


    // statistics-related function
    //----------------------------

    public List<DhtRouterList> getDhtRouterList( ID rid ) {
        return proxyDht.getDhtRouterList( rid );
    }

   
    // observer intereface
    //--------------------

    public void update( Observable o, Object arg ) {
        
        // this will be called when new native record added
        //-------------------------------------------------

        DhtRouterMessage message = (DhtRouterMessage) arg;

        Log log = LogFactory.getLog(DhtRouter.class);
        log.info( "update called=" + rsc.getProvider() + 
                  " (msg=" + message.getMsg() + ")" );
        
        Record record = message.getRecord();
        RemoteServer server = message.getRemoteServer();

        log.info( "  DhtRouter.update: " );

	    String address = "";

	    if ( server != null ){
	        log.info( "  remote server=" + server.getAddress() );
	        address = server.getAddress();
	    } else {
	        log.info( "  remote server=SELF" );
	        String url = proxyDht.getProxyHost() + ":" + WSContext.getPort();
	        address = rsc.getProxyProto().getAddress();
	        address = address.replaceAll( "%%URL%%", url );
	    }

	    log.info( "  NS=" + record.getNs() + 
                  "  AC=" + record.getAc() +
                  "  service=" + record.getService() );
        
        ID rid = this.getRecordID( record.getService(), 
                                   record.getNs(), 
                                   record.getAc() );

	    log.info( "  DhtRouterItem: address=" + address +
		          " create=" + record.getCreateTime().getTime() +
		          " expire=" + record.getExpireTime().getTime() );
	
        DhtRouterItem routerItem =
            new DhtRouterItem( address,
                               record.getCreateTime().getTime(),
                               record.getExpireTime().getTime() );
        
        if ( message.getMsg() == DhtRouterMessage.DELETE ) {
	        log.info( "  DhtRouterItem: DELETING" );
            proxyDht.deleteIterm( rid, routerItem );
        } 
        
        if ( message.getMsg() == DhtRouterMessage.UPDATE ) {
	        log.info( "  DhtRouterItem: UPDATING" );
            proxyDht.updateIterm( rid, routerItem );
        }
    }
}
