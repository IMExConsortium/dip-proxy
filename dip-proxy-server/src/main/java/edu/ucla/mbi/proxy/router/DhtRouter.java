package edu.ucla.mbi.proxy.router;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * DhtRouter:
 *   selects remote proxy server to call  
 *
 *=========================================================================== */

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

import edu.ucla.mbi.server.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;

public class DhtRouter implements Router {
    
    private RemoteServerContext rsc = null;
    private Dht proxyDht = null;
    
    private NativeServer currentServer = null;
    
    private DhtRouter() { }
    
    private DhtRouter( RemoteServerContext rsc,  Dht dht ){ 
        this.rsc = rsc;
        this.proxyDht = dht;        
    }
    
    public Router createRouter(){
        return new DhtRouter( this.rsc, this.proxyDht);
    }
    
    public void setDht( Dht dht ){
        this.proxyDht = dht;
    }
    

    public void setRemoteServerContext( RemoteServerContext rsc ){
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info(" setRemoteServerContext=" + rsc );
        this.rsc = rsc;
    }

    public RemoteServerContext getRemoteServerContext(){
        return rsc;
    }
    
    private ID getRecordID( String service, 
                           String namespace,
                           String accession ){
        
        String recordStrId = rsc.getProvider() + ":" + service +
            namespace + ":" + accession + ":";
        
        return ID.getSHA1BasedID( recordStrId.getBytes() );
    }
    
    // statistics-related function
    //----------------------------

    public List<DhtRouterList> getDhtRouterList( String provider,
                                                 String service,
                                                 String namespace,
                                                 String accession ){
        
        ID rid = this.getRecordID( provider, service,
                                   namespace, accession );  
        return proxyDht.getDhtRouterList( rid );
    }    

    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------
    // observer intereface
    //--------------------

    public void update( Object rns, Object arg ) {
        
        //  called when new native record added
        //--------------------------------------
        
        DhtRouterMessage message = (DhtRouterMessage) arg;

        Log log = LogFactory.getLog( DhtRouter.class );
        log.info( "update called=" + rsc.getProvider() + 
                  " (msg=" + message.getMsg() + ")" );
        
        NativeRecord record = message.getRecord();
       
        log.info( "  DhtRouter.update: " );
        
        String address = "";
        
        log.info( "local proxy address:" );

        String url = proxyDht.getProxyHost() + ":" + WSContext.getPort();
         
        address = rsc.getProxyProto().getAddress();
        address = address.replaceAll( "%%URL%%", url );
        
        log.info( "Record: provider=" +record.getProvider() + 
                  " service=" + record.getService() +
                  " NS=" + record.getNs() + 
                  " AC=" + record.getAc() );
        
        ID rid = this.getRecordID( record.getProvider(),
                                   record.getService(), 
                                   record.getNs(), 
                                   record.getAc() );
        
        log.info( "  DhtRouterItem: address=" + address +
                  " query=" + record.getQueryTime().getTime() +
                  " expire=" + record.getExpireTime().getTime() );
	
        DhtRouterItem routerItem =
            new DhtRouterItem( address,
                               record.getQueryTime().getTime(),
                               record.getExpireTime().getTime() );
        
        if ( message.getMsg() == DhtRouterMessage.DELETE ) {
            log.info( "  DhtRouterItem: DELETING" );
            proxyDht.deleteItem( rid, routerItem );
        } 
        
        if ( message.getMsg() == DhtRouterMessage.UPDATE ) {
            log.info( "  DhtRouterItem: UPDATING" );
            proxyDht.updateItem( rid, routerItem );
        }
    }
        
    public NativeServer getNextProxyServer( String provider, 
                                            String service, 
                                            String namespace,
                                            String accession ) {
        
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info( "getNextProxyServer(args) (provider=" + 
                  rsc.getProvider() + ")" );
        log.info( " SRV=" + service + 
                  " NS=" + namespace + " AC=" + accession );
        
        ID rid = this.getRecordID( provider, service, namespace, accession );
        
        return this.getNextProxyServer( rid, service );
    }

    //--------------------------------------------------------------------------
    
    private NativeServer getNextProxyServer( ID rid, String service ) {
        
        Log log = LogFactory.getLog(DhtRouter.class);
        log.info( "  rid=" + rid.toString(16) + " @ " + proxyDht );        
        RemoteProxyServer pserver = null;
        NativeServer remote = null;
        
        String lastAddress = proxyDht.getLastAddress( rid );
        
        log.info( "lastAddress=" + lastAddress );

        // local proxy address/port
        //-------------------------
        String url = proxyDht.getProxyHost() + ":" + WSContext.getPort();
       
        log.info( "url="+ url );
        
        String localAddress = rsc.getProxyProto().getAddress();
        
        localAddress = localAddress.replaceAll( "%%URL%%", url );
        
        log.info( "localAddress=" + localAddress );
        
        if ( lastAddress != null && 
             ! ( lastAddress.equals( localAddress ) ) ) {
           
            log.info( "lastAddress not equals localAddress. " ); 
            RemoteProxyServer remoteProxy =
                rsc.getProxyProto().getRemoteProxyServerInstance();
            remoteProxy.setAddress( lastAddress );
            remote = remoteProxy;
            log.info( "   remote URL=" + lastAddress );
            
        } else {            
            remote = rsc.getNativeServer();

            log.info( "lastAddress same as localAddress:" );
            log.info( "   remote==native " + remote );
        }
        
        currentServer = remote;
        return remote;
    }   

    private ID getRecordID( String provider,
                            String service,
                            String namespace,
                            String accession ){
        
        String recordStrId = provider + ":" + service +
            namespace + ":" + accession + ":";
        
        return ID.getSHA1BasedID( recordStrId.getBytes() );
    }

}
