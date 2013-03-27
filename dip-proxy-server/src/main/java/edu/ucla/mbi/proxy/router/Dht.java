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

    public final static int MAX_DRL_SIZE = 2;
    
    private Properties dhtProperties = null;
    
    private String overlayMode = "networked";
    private String dhtPort = null;
    private String routingAlg = null;
    private String directoryType = null;
    private String workingDirectory = null; 
    private String proxyHost = null;
    private int maxDrlSize = MAX_DRL_SIZE;
    private long defaultTTL = 0;

    private List<String> bootServers = null;

    private DHT proxyDht = null;

    public Dht() { }
    
    public void setRoutingAlgorithm( String algorithm ){
        this.routingAlg = algorithm;
    }

    public void setDirectoryType( String dirType ) {
        this.directoryType = dirType;
    }

    public void setWorkingDirectory ( String workingDir ) {
        this.workingDirectory = workingDir;
    }

    public void setDhtPort( String port ){
        this.dhtPort = port;
    }

    public void setMaxDrlSize ( int size ) {
        this.maxDrlSize = size;
    }

    public void setDefaultTTL ( long ttl ) {
        this.defaultTTL = ttl * 60 * 60 * 1000;
    }

    public String getRoutingAlgorithm(){
        return this.routingAlg;
    }

    public String getDirectoryType(){
        return this.directoryType;
    }

    public String getWorkingDirectory(){
        return this.workingDirectory;
    }

    public String getProxyHost(){
        return this.proxyHost;
    }

    public String getDhtPort(){
        return this.dhtPort;
    }

    public int getMaxDrlSize() {
        return this.maxDrlSize;
    }

    public long getDefaultTTL () {
        return this.defaultTTL;
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
        log.info( " defaultTTL=" + defaultTTL );

        String proxyHome = System.getProperty( "dip.proxy.home");
        log.info( " proxyHome=" + proxyHome );

        if( !workingDirectory.startsWith( File.separator )  
            && proxyHome != null && !proxyHome.isEmpty() ) {

            workingDirectory = proxyHome + File.separator + workingDirectory;
        }     

        log.info( "workingDir= " + workingDirectory );

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
            
            if( routingAlg != null && !routingAlg.isEmpty() ){
                dhtc.setRoutingAlgorithm( routingAlg );
            }
            // set directory type/location
            //----------------------------
            if( directoryType != null && !directoryType.isEmpty() ) {
                dhtc.setDirectoryType( directoryType );
            }

            if( workingDirectory != null && !workingDirectory.isEmpty() ) {
                dhtc.setWorkingDirectory( workingDirectory );
            }

            if( defaultTTL != 0 ){
                dhtc.setDefaultTTL ( defaultTTL );
            }

            // propagate old records to incoming nodes ? 
            //-------------------------------------------
            
            dhtc.setDoReputOnReplicas( true );

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
                            log.info( "   routing exception: " + re.toString() );
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
                    log.info( "   routing exception: " + re.toString() );
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
   
    /* 
    public Set<ValueInfo<DhtRouterList>> getDhtRecord( ID rid ){
        
        Log log = LogFactory.getLog( Dht.class );
        Set<ValueInfo<DhtRouterList>> val = null;
        try {
            val = proxyDht.get(rid);
        } catch( RoutingException re ) {
            log.info( "  routing exception: " + re.toString() );
            return null;
        }
        
        return val;
    } */

    
    public DhtRouterList getDhtRouterList( ID rid ) {

        Log log = LogFactory.getLog( Dht.class );
        Set<ValueInfo<DhtRouterList>> vis = null;

        try {
            vis = proxyDht.get(rid);
        } catch( RoutingException re ) {
            log.warn( "Fault: routing exception: " + re.toString() );
            return null;
        }
        
        log.info( " got value info set ..." );

        if( vis == null || vis.size() == 0 ) return null;

        /*
            if( vis.size() != 1 ) {
                log.info( "Fault: DHT return empty set or " +
                          "more DhtRouterList. " );
                return null;
            }

            ValueInfo<DhtRouterList> vi = vis.iterator().next();
            DhtRouterList drl = vi.getValue();
            log.info( " DhtRouterList=" + drl );
            return drl;
        */
            
        DhtRouterList newDrl = new DhtRouterList( rid );

        for( Iterator<ValueInfo<DhtRouterList>> i = vis.iterator();
             i.hasNext(); ){
            
            ValueInfo<DhtRouterList> vi = i.next();
            
            log.info( "  list=" + vi.getValue() );
            
            DhtRouterList drl = vi.getValue();
            
            log.info( " DhtRouterList=" + drl );

            int count = 0;
            for( Iterator<DhtRouterItem> pi = drl.iterator();
                 pi.hasNext(); ){
                
                DhtRouterItem item = pi.next();
                count++;
                log.info( "count=" + count + ":" );
                log.info( "item=" + item.toString() );
                log.info( "item.ExpireTime=" + item.getExpireTime() );
                log.info( "item.CreateTime=" + item.getCreateTime());
                
                // check if item is not expired. ignore expired itenms
                
                if( newDrl.contains( item ) ) {
                    log.info( "newDrl contains item. " );
                    int index = newDrl.indexOf( item );
                    DhtRouterItem oldItem = newDrl.getItem( index );
                    if( oldItem.getCreateTime() < item.getCreateTime() ) {
                        log.info( "newDrl set the item. " ); 
                        //*** keep most recently item
                        newDrl.setItem ( index, item );
                    }
                } else {
                    log.info( "newDrl add the item. " );
                    newDrl.addItem ( item );
                }
            }
        }
        
        log.info( "getDhtRouterList: newDrl=" + newDrl );
        return newDrl;
    }
    
    public void updateItem( ID rid, DhtRouterItem newItem ) {
        
        Log log = LogFactory.getLog(Dht.class);

        log.info( "updateItem entering with newItem=" + newItem );

        DhtRouterList drl = getDhtRouterList( rid );
        
        boolean newFlag = true;
      
        if( drl == null ) {
            drl = new DhtRouterList( rid );
            drl.addItem( newItem );
        } else {

            long firstQuery = 0;
            DhtRouterItem firstQueryItem = null ;

            for( int i = 0; i < drl.size(); i++ ) {
            
                DhtRouterItem proxyItem = drl.getItem(i);                   

                log.info( "   item=" + proxyItem.toString() );
                
                if ( proxyItem.getAddress().equals(newItem.getAddress()) ){
                    newFlag = false;
                    proxyItem.setCreateTime( newItem.getCreateTime() );
                    proxyItem.setExpireTime( newItem.getExpireTime() );
                    drl.setItem ( i, proxyItem );
                    break;
                }

                if( firstQuery == 0  
                    || proxyItem.getCreateTime() < firstQuery ) {
                        
                    firstQuery = proxyItem.getCreateTime();
                    firstQueryItem = proxyItem;
                }
            }

            if( newFlag ) {
                log.info( "updateItem: newFlag=true: add newItem=" + newItem );
                DhtRouterItem dpi = newItem;
                drl.addItem( dpi );
                    
                if ( drl.size() > maxDrlSize ){
                    drl.removeItem( firstQueryItem );
                }
            }
        } 
        
        try {

            proxyDht.setHashedSecretForPut( new ByteArray( rid.getValue() ) );

            proxyDht.put( rid, drl );

            log.info( "updateItem: after put: dht rc=" + proxyDht.get(rid) );

        } catch ( Exception e ) {
            log.info( "dht exception:" + e.toString() );
        }

        log.info( "update(UPDATE): done" );
    }

    public void deleteItem( ID rid, DhtRouterItem newItem ) {
        
        Log log = LogFactory.getLog(Dht.class);

        DhtRouterList drl = getDhtRouterList( rid );
                 
        boolean removeFlag = false;
 
        if( drl != null ) {
            for( int i = 0; i < drl.size(); i++ ) {
                    
                DhtRouterItem proxyItem = drl.getItem( i );                   
                log.info( "   item=" + proxyItem.toString() );
                    
                if( proxyItem.getAddress().equals(newItem.getAddress()) ){
                    drl.removeItem( i );
                    removeFlag = true; 
                    break;
                }
            }
        }
        
        if( removeFlag ) {        
            try {
                if( drl.size() == 0 ) {
                    // remove empty drl
                    proxyDht.remove( rid, new ByteArray( rid.getValue() ) );
                } else {     
                    //update dht value
                    proxyDht.setHashedSecretForPut( new ByteArray( rid.getValue() ) );
                    proxyDht.put( rid, drl );
                }     
            } catch ( Exception e ) {
                log.info( "dht exception:" + e.toString() );
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

        DhtRouterList drl = getDhtRouterList( rid );        

        DhtRouterList nDrl =  new DhtRouterList( rid );
        
        boolean removeFlag = false;

        if( drl == null ) {
            return null;
        }
       
        for( int i = 0; i < drl.size(); i++ ) {

            DhtRouterItem item = drl.getItem( i );                    
            log.info( "getlastAddress: item=" + item.toString() );
        
            long now = Calendar.getInstance().getTimeInMillis();

            if( item.getExpireTime() > now ) {
                if ( item.getCreateTime() > lastCreate ){
                    lastCreate = item.getCreateTime();
                    lastCrUrl = item.getAddress();
                }
                nDrl.addItem( item );
            } else {
                removeFlag = true;
            }
        }

        if( removeFlag ) {
            try {   
                if( nDrl.size() == 0 ) {
                    // remove empty drl
                    proxyDht.remove( rid, new ByteArray( rid.getValue() ) );
                } else {
                    // update dht record
                    proxyDht.setHashedSecretForPut( new ByteArray( rid.getValue() ) );
                    proxyDht.put( rid, nDrl );
                }
            } catch ( Exception e ) {
                log.info( "dht exception:" + e.toString() );
            }
        }
        log.info( "return addres=" + lastCrUrl );
        return lastCrUrl;
    }   

    public List<DhtRouterList> getDhtRouterList( String provider,
                                                 String service,
                                                 String namespace,
                                                 String accession ) {
        
        ID rid = getRecordID( provider, service,
                              namespace, accession );
           
        List<DhtRouterList> drl = new ArrayList<DhtRouterList>();
        drl.add( getDhtRouterList( rid ) );
        
        return drl;
    }


    
    /*     
    
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
    */ 
}
