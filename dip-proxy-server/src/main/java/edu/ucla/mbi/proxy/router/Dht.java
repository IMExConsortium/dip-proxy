package edu.ucla.mbi.proxy.router;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * Dht:
 *   DHT access 
 *
 *============================================================================ */

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

import edu.ucla.mbi.server.WSContext;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;

public class Dht {

    public final static int MAX_DPL_SIZE = 2;
    
    private Properties dhtProperties = null;
    
    private String overlayMode = "networked";
    private String dhtPort = null;
    private String routingAlg = null;

    private String proxyHost = null;


    private List<String> bootServers = null;

    private DHT proxyDht = null;

    public Dht() { }
    
    public void setRoutingAlgorithm( String algorithm ){
        this.routingAlg = algorithm;
    }

    public String getRoutingAlgorithm() {
        return this.routingAlg;
    }

    public void setDhtPort( String port ){
        this.dhtPort = port;
    }

    public String getProxyHost() {
        return this.proxyHost;
    }

    public String getDhtPort() {
        return this.dhtPort;
    }

    public void setBootServers( List bootServers ){
        this.bootServers = (List<String>) bootServers; 
    }

    public List<String> setBootServers() {
        return this.bootServers;
    }

    public void setOverlayMode( String mode ) {
        
        Log log = LogFactory.getLog( Dht.class );
        
        if ( mode != null ){
            if ( mode.equalsIgnoreCase( "networked" ) ) {
                overlayMode = "networked";
            } 
            /*
            else if ( mode.equalsIgnoreCase( "local" ) ) {
                overlayMode = "local";
            }*/
            else {
                overlayMode = "local";
            }
            log.info(   "overlay mode= " + overlayMode );
        } else {
            log.info( "overlay mode=" + overlayMode + " (default)" );
        }
    }

    public String getOverlayMode() {
        return this.overlayMode;
    }

    public DHT getDHT(){
        return this.proxyDht;
    }

    //--------------------------------------------------------------------------

    public void initialize() {
        Log log = LogFactory.getLog( Dht.class );
        log.info( "initializing(mode=" + routingAlg +")" );
        log.info( " boot servers=" + bootServers);
        
        String proxyTime  = null;
        String proxyIdStr = null;
        BigInteger proxyIdSHA1 = null;

        ID proxyId = null;

        dhtProperties = new Properties();

        try{
            InputStream fis = new FileInputStream("dhtrouter.properties"); 
            
            dhtProperties.load( fis );
            
            dhtPort = dhtProperties.getProperty( "dht-port" );
            
            proxyHost = dhtProperties.getProperty("proxy-host");
            proxyIdStr = dhtProperties.getProperty("proxy-str-id");
            proxyIdSHA1 =  
                new BigInteger( dhtProperties.getProperty( "proxy-sha1-id" ), 
                                16 );
            proxyId =
                ID.getID( proxyIdSHA1, 20 );
            log.info( " old: proxyIdStr=" + proxyIdStr );
            log.info( " old:    proxyId=" + proxyIdSHA1.toString(16) );
            
        } catch (FileNotFoundException e ){

            try {
                InetAddress localHost = InetAddress.getLocalHost();
                //proxyHost = localHost.getHostName();
                proxyHost = localHost.getHostAddress();
                
            } catch (UnknownHostException ue) {
                log.info( " unknown host...");
                proxyHost = "dip-proxy" ;
            }
            
            Calendar now = Calendar.getInstance();
            proxyTime = Long.toString( now.getTimeInMillis() );
            
            proxyIdStr = proxyHost + ":" + dhtPort + ":" + proxyTime;
            proxyId = ID.getSHA1BasedID( proxyIdStr.getBytes() );
            proxyIdSHA1 = proxyId.toBigInteger();
            
            log.info( " new: proxyIdStr=" + proxyIdStr );
            log.info( " new:    proxyId=" + proxyIdSHA1.toString(16) );
       
            dhtProperties.setProperty( "dht-port", dhtPort );     
            dhtProperties.setProperty( "proxy-host", proxyHost );            
            dhtProperties.setProperty( "proxy-str-id", proxyIdStr );
            dhtProperties.setProperty( "proxy-sha1-id", 
                                       proxyIdSHA1.toString(16) );
            try {
                OutputStream fos =
                    new FileOutputStream( "dhtrouter.properties" );
                
                dhtProperties.store( fos, 
                                     "Autogenerated during initial startup."+
                                     " Remove to reinitialize." ); 
                
                fos.close();
            } catch ( FileNotFoundException fnf ){
                log.info( "  dhtrouter.properties: " +
                          "cannot create(FileNotFoundException)");
            } catch ( IOException ioe){
                log.info( "  dhtrouter.properties: " +
                          "cannot create(IOException)");
            }
        } catch (Exception uhe) {
            log.info( " unknown exception...");
        }

        try {
            DHTConfiguration dhtc = DHTFactory.getDefaultConfiguration();

            // set hash value type
            //--------------------

            dhtc.setValueClass( DhtRouterList.class );

            // set port
            //---------

            log.info( "  DHTConfiguration: port=" + Integer.parseInt( dhtPort ) );

            dhtc.setSelfPort( Integer.parseInt( dhtPort ) );
            dhtc.setContactPort( Integer.parseInt( dhtPort ) );
            
            // turn-off UPnP
            //--------------

            dhtc.setDoUPnPNATTraversal( false );

            // use UDP (default)
            //------------------

            dhtc.setMessagingTransport( "UDP" );

            // set routing algorithm
            //----------------------

            log.info( "  DHTConfiguration: RoutingAlgorithm=" +  routingAlg );
            
            dhtc.setRoutingAlgorithm( routingAlg );
            
            proxyDht = DHTFactory.getDHT( dhtc, proxyId );
                        
            // join overlay
            //-------------

            log.info( " joining overlay" );

            MessagingAddress ma = null;
            InetAddress localAddress = null;
            try {
                localAddress = InetAddress.getLocalHost();
            } catch( UnknownHostException e ){
                log.info( " should not happen " );                
            }
            
            if ( overlayMode.equalsIgnoreCase( "networked" ) ){
                
                for( Iterator<String> i = bootServers.iterator(); 
                     i.hasNext(); ) {
                
                    String bootHost = i.next();
                
                    log.info( "  local host=" +
                              localAddress.getHostAddress() );
                    
                    if ( ! bootHost.equals( localAddress.getHostAddress() )) { 
                        
                        log.info( "  trying (non-self) boothost=" + 
                                  bootHost + ":" + dhtPort );
                    
                        try {
                            ma = proxyDht
                                .joinOverlay( bootHost 
                                              + ":" + dhtPort,  
                                              Integer.parseInt( dhtPort ) );
                            log.info( "overlay joined (MessagingAddress=" + 
                                      ma + ")" );
                            break;
                        } catch ( ow.routing.RoutingException re ) {
                            log.info( "   routing exception: " + re );
                        }                 
                    } else {
                        log.info( "  skipping (non-local) boothost=" + 
                                  bootHost + ":" + dhtPort );
                    }
                }
            }
            
            if( bootServers.contains( localAddress.getHostAddress() )
                && ma == null ) {
                try {
                    ma = proxyDht
                        .joinOverlay( localAddress.getHostAddress() 
                                      + ":" + dhtPort,  
                                      Integer.parseInt( dhtPort ) );

                    log.info( "overlay started (MessagingAddress=" + 
                              ma + ")" );
                    
                } catch ( ow.routing.RoutingException re ) {
                    log.info( "   routing exception: " + re );
                }
            }                 
            
        } catch( Exception e ){
            e.printStackTrace();
        }
    }
    
    //--------------------------------------------------------------------------
    
    public void cleanup() {
        
        Log log = LogFactory.getLog( Dht.class );
        log.info( "Dht.cleanup(proxyDht=" + proxyDht +")" );
        
        if( proxyDht != null ){
            proxyDht.stop();
        }
    }
    
    //--------------------------------------------------------------------------

    public ID getRecordID( String provider,
                           String service,
                           String namespace,
                           String accession ){
        
        String recordStrId = provider + ":" + service +
            namespace + ":" + accession + ":";
        
        return ID.getSHA1BasedID( recordStrId.getBytes() );
    }
    
    public Set<ValueInfo<DhtRouterList>> getDhtRecord( ID rid ){
        
        Log log = LogFactory.getLog( Dht.class );
        Set<ValueInfo<DhtRouterList>> val = null;
        try {
            val = proxyDht.get(rid);
        } catch( RoutingException re ) {
            log.info( "  routing exception" );
            return null;
        }
        
        return val;
    }
    
    public void updateItem( ID rid, DhtRouterItem newItem ) {
        
        Log log = LogFactory.getLog(Dht.class);

        Set<ValueInfo<DhtRouterList>> val = null;
        RemoteServer remote = null;

        try {

            val = proxyDht.get(rid);

        } catch( RoutingException re ) {
            log.info( "  UpdateItem: routing exception" + re.toString() );
            re.printStackTrace();
        } catch( Exception ex ) {
            log.info( "UpdateItem: generic exception" + ex.toString() );
            ex.printStackTrace();
        }
        
        log.info( " got val=" + val );
        boolean newFlag = true;
        
        if ( val != null && val.size() > 0 ) {
            for( Iterator<ValueInfo<DhtRouterList>> i = val.iterator();
                 i.hasNext(); ){
            
                ValueInfo<DhtRouterList> vi = i.next();
                
                log.info( "  list=" + vi.getValue() );
                
                DhtRouterList dpl = vi.getValue();
                DhtRouterItem lastItem = null;
                long lastExpire = 0;
                String lastUrl = null;
                
                for ( Iterator<DhtRouterItem> pi = dpl.iterator();
                      pi.hasNext(); ){
                    
                    DhtRouterItem proxyItem = pi.next();                   
                    log.info( "   item=" + proxyItem.toString() );
                    
                    if ( proxyItem.getAddress().equals(newItem.getAddress()) ){
                        newFlag = false;
                        proxyItem.setCreateTime( newItem.getCreateTime() );
                        proxyItem.setExpireTime( newItem.getExpireTime() );
                    }
                }

                if( newFlag ) {
                    DhtRouterItem dpi = newItem;
                    dpl.addItem( dpi );
                    
                    if ( dpl.size() > MAX_DPL_SIZE ){
                        dpl.removeItem( 0 );
                    }
                }   
                
                try {
                    proxyDht.put( rid, dpl );            
                } catch ( Exception e ) {
                    log.info( "dht exception:" + e.toString() );
                }
                
            }
        } else {
            DhtRouterList dpl = new DhtRouterList();
            DhtRouterItem dpi = newItem;
            dpl.addItem( dpi );
            
            try {
                proxyDht.put( rid, dpl );
            } catch ( Exception e ) {
                log.info( "dht exception:" + e.toString() );
            } 
        }
        log.info( "update(UPDATE): done" );
    }

    public void deleteItem( ID rid, DhtRouterItem newItem ) {
        
        Log log = LogFactory.getLog(Dht.class);
        Set<ValueInfo<DhtRouterList>> val = null;
        RemoteServer remote = null;
        
        try {
            val = proxyDht.get(rid);            
        } catch( RoutingException re ) {
            log.info( "  routing exception" );
        }
        log.info( " got val..." );
     
        if( val != null && val.size() > 0 ) {
            for( Iterator<ValueInfo<DhtRouterList>> i = val.iterator();
                 i.hasNext(); ){
                
                ValueInfo<DhtRouterList> vi = i.next();
                
                log.info( "  list=" + vi.getValue() );
                
                DhtRouterList dpl = vi.getValue();
                DhtRouterItem lastItem = null;
                long lastExpire = 0;
                String lastUrl = null;
                
                for( Iterator<DhtRouterItem> pi = dpl.iterator();
                      pi.hasNext(); ){
                    
                    DhtRouterItem proxyItem = pi.next();                   
                    log.info( "   item=" + proxyItem.toString() );
                    
                    if( proxyItem.getAddress().equals(newItem.getAddress()) ){
                        dpl.removeItem( proxyItem );
                        log.info( "   item removed" );
                        break;
                    }
                }

                try {
                    proxyDht.put( rid, dpl );            
                } catch ( Exception e ) {
                    log.info( "dht exception:" + e.toString() );
                }
            }
        } 
        log.info( "update(DELETE): done" );
    }
    
    public String getLastAddress( ID rid ){
        
        long lastCreate = 0;
        long lastExpire = 0;

        String lastCrUrl = null;
        String lastExUrl = null;

        Log log = LogFactory.getLog(Dht.class);

        Set<ValueInfo<DhtRouterList>> dhtRec= null;
       
        try {
            dhtRec = proxyDht.get(rid);
        } catch( RoutingException re ) {
            log.info( "  routing exception" + re.toString() );
            re.printStackTrace();
        } catch( Exception ex ){
            log.info( "  exception" + ex.toString() );
            ex.printStackTrace();
        }
        
        log.info( "  DHT Record(s) retieved=" + dhtRec );
        
        if( dhtRec != null && dhtRec.size() > 0 ){
            
            log.info( "  old key. checking..." );
            
            for( Iterator<ValueInfo<DhtRouterList>> i = dhtRec.iterator(); 
                 i.hasNext(); ){

                ValueInfo<DhtRouterList> vi = i.next();
                
                log.info( "  list=" + vi.getValue() );
                
                DhtRouterList dpl = vi.getValue();
                DhtRouterItem lastItem = null;
                
                for ( Iterator<DhtRouterItem> pi = dpl.iterator(); 
                      pi.hasNext(); ){
                    
                    DhtRouterItem item = pi.next();
                    
                    log.info( "   item=" + item.toString() );
                    log.info( "   item.ExpireTime=" + item.getExpireTime() );
                    log.info( "   item.CreateTime=" + item.getCreateTime());
                    
                    if ( item.getExpireTime() > lastExpire ){
                        lastExpire = item.getExpireTime();
                        lastExUrl = item.getAddress();
                    }
                    
                    if ( item.getCreateTime() > lastCreate ){
                        lastCreate = item.getCreateTime();
                        lastCrUrl = item.getAddress();
                    }
                }
            }
        }
        log.info( "   return addres=" + lastExUrl );
        return lastExUrl;
    }   

    public List<DhtRouterList> getDhtRouterList( String provider,
                                                 String service,
                                                 String namespace,
                                                 String accession ) {
        
        ID rid = getRecordID( provider, service,
                              namespace, accession );
           
        return getDhtRouterList( rid );
    }
    
    
    public List<DhtRouterList> getDhtRouterList( ID rid ){
        
        Log log = LogFactory.getLog( Dht.class );
        
        Set<ValueInfo<DhtRouterList>> dhtRec= null;
        List<DhtRouterList> dhtList = new ArrayList<DhtRouterList>();
        
        try {
            dhtRec = proxyDht.get(rid);  
        }catch( RoutingException re ) {
            log.info( "  routing exception "+ re.toString() );
        } catch( Exception ex ) {
            log.info( "  routing exception "+ ex.toString() );
        }
        
        if( dhtRec != null && dhtRec.size() > 0 ){

            for( Iterator<ValueInfo<DhtRouterList>> i = dhtRec.iterator(); 
                 i.hasNext(); ){

                ValueInfo<DhtRouterList> vi = i.next();
                
                log.info( "  list=" + vi.getValue() );
                
                DhtRouterList dpl = vi.getValue();
                dhtList.add( dpl );
            }
        }
        
        return dhtList;
    }   
}
