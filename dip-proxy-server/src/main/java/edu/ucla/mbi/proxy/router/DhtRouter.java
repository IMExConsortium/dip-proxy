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
        
        List<DhtRouterList> drl = new ArrayList<DhtRouterList>();
        drl.add( proxyDht.getDhtRouterList( rid ) );
        return drl; 
    }    

    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------
    // observer interface
    //--------------------

    public void update( Object rns, Object arg ) {
        
        //  called when new native record added
        //--------------------------------------
        
        DhtRouterMessage message = (DhtRouterMessage) arg;

        Log log = LogFactory.getLog( DhtRouter.class );
        log.info( "update called=" + rsc.getProvider() + 
                  " (msg=" + message.getMsg() + ")" );
        
        NativeRecord record = message.getRecord();
        NativeServer server = message.getNativeServer();
        
        log.info( "Record: provider=" +record.getProvider() + 
                  " service=" + record.getService() +
                  " NS=" + record.getNs() + 
                  " AC=" + record.getAc() );
        
        ID rid = this.getRecordID( record.getProvider(),
                                   record.getService(), 
                                   record.getNs(), 
                                   record.getAc() );
        
        if ( message.getMsg() == DhtRouterMessage.UPDATE ) {
            
            log.info( "  DhtRouter.update: " );
        
            String address = "";
        
            log.info( "local proxy address:" );

            String url = proxyDht.getProxyHost() + ":" + WSContext.getPort();
            
            //address = rsc.getProxyProto().getAddress();
            address = this.getLocalAddress();

            //address = address.replaceAll( "%%URL%%", url );
            log.info( "update: UPDATE: address=" + address );

            DhtRouterItem routerItem =
                new DhtRouterItem( address,
                                   record.getQueryTime().getTime(),
                                   record.getExpireTime().getTime() );
            
            log.info( "  DhtRouterItem: address=" + address +
                      " query=" + record.getQueryTime().getTime() +
                      " expire=" + record.getExpireTime().getTime() );
            
            log.info( "  DhtRouterItem: UPDATING" );
            proxyDht.updateItem( rid, routerItem );
        }

        if ( message.getMsg() == DhtRouterMessage.DELETE ) {
            
            String address = null;

            if( server instanceof RemoteProxyServer ) {
                address = ((RemoteProxyServer) server).getAddress();
            } else {
                //address = local address 
                address = this.getLocalAddress();
            }
        
            log.info( "remote proxy address=" + address  );

            if( address != null && !address.isEmpty() ) {

                log.info( "deleted record=" + record );
            
                DhtRouterItem routerItem =
                    new DhtRouterItem( address,
                                       record.getQueryTime().getTime(),
                                       record.getExpireTime().getTime() );
                
                log.info( "deleted routerItem=" + routerItem ); 
                log.info( " DhtRouterItem: address=" + address +
                          " query=" + record.getQueryTime().getTime() +
                          " expire=" + record.getExpireTime().getTime() );
        
                log.info( "  DhtRouterItem: DELETING" );

                proxyDht.deleteItem( rid, routerItem );
            }
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
        NativeServer remote = null;
        
        String lastAddress = proxyDht.getLastAddress( rid );
        
        log.info( "lastAddress=" + lastAddress );

        /*
        // local proxy address/port
        //-------------------------
        String url = proxyDht.getProxyHost() + ":" + WSContext.getPort();
       
        log.info( "url="+ url );
        
        String localAddress = rsc.getProxyProto().getAddress();
        
        localAddress = localAddress.replaceAll( "%%URL%%", url );
        
        log.info( "localAddress=" + localAddress );
        */

        String localAddress = this.getLocalAddress();

        if ( lastAddress != null && 
             ! ( lastAddress.equals( localAddress ) ) ) {
           
            log.info( "lastAddress not equals localAddress. " ); 
            log.info( "remote come from RemoteProxyServer. " );
            remote = rsc.getProxyProto().getRemoteProxyServerInstance( 
                                                                lastAddress );
            log.info( "   remote URL=" + lastAddress );
            
        } else {            
            log.info( "remote from NativeServer. " );
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

    private String getLocalAddress () {

        Log log = LogFactory.getLog(DhtRouter.class);

        // local proxy address/port
        //-------------------------
        String url = proxyDht.getProxyHost() + ":" + WSContext.getPort();

        log.info( "url="+ url );

        String localAddress = rsc.getProxyProto().getAddress();

        localAddress = localAddress.replaceAll( "%%URL%%", url );

        log.info( "localAddress=" + localAddress );
        return localAddress ;
    }
}
