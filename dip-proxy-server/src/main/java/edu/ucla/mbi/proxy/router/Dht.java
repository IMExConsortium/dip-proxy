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

import edu.ucla.mbi.proxy.context.WSContext;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.context.*;


public class Dht implements ContextListener {
//public class Dht {
    private final static int MAX_DRL_SIZE = 2;

    private final String propertiesFN = "/tmp/dhtrouter.properties";
   
    private Map<String, Object> jsonOptionDefMap = new HashMap();
 
    private Properties dhtProperties = null;
    
    private String overlayMode = "networked";
    private String dhtPort = "55666";
    private String routingAlg = "Chord";
    private String directoryType = "BerkeleyDB";
    private String workingDirectory = "dht"; 
    private int maxDrlSize = MAX_DRL_SIZE;
    private long defaultTTL = 0;
    private List<String> bootServerList = null;

    private String proxyHost = null;

    private DHT proxyDht = null;

    private short applicationID = 2;
    private short applicationVersion = 2;

    public Dht() { }

    private JsonContext dhtContext;

    public void setDhtContext( JsonContext context ){
        this.dhtContext = context;
    }
   
    private void readDhtContext() throws ServerFault {
        
        Log log = LogFactory.getLog( Dht.class );
        log.info( "readDhtContext... " );

        FileResource fr = (FileResource) dhtContext
                                .getConfig().get("json-source");

        if ( fr == null ) return;

        try {
            dhtContext.readJsonConfigDef( fr.getInputStream() );
        } catch ( Exception e ){
            log.warn( "initialize exception: " + e.toString() );
            throw ServerFaultFactory.newInstance ( Fault.JSON_CONFIGURATION );
        }

        Map<String, Object> dhtJsonMap = dhtContext.getJsonConfig();

        log.info( "before retrieveOptionDef... " );

        if ( dhtJsonMap.get( "option-def" ) != null ) {        
            retrieveOptionDef ( dhtJsonMap );
        } else {
            throw ServerFaultFactory
                .newInstance( Fault.JSON_CONFIGURATION );
        }
        
        log.info( "before setDhtProperty... " );
        setDhtProperty();
    }
    
    private void retrieveOptionDef( Map<String, Object> jsonMap ) {
  
        Map<String,Object> optionDef = 
            (Map<String,Object>) jsonMap.get( "option-def" );
                
        Set<String> newDefs = optionDef.keySet();
        
        for( Iterator<String> is = newDefs.iterator(); is.hasNext(); ){
            
            String key = is.next();
            
            Map<String, Object> def = 
                (Map<String, Object>)optionDef.get( key );
            
            if( def.get( "value" ) != null ) {
                jsonOptionDefMap.put( key, def );  
            }
        
            if( def.get( "option-def" ) != null ){
                retrieveOptionDef( def );
            }
        }                    
    }

    
    public void contextUpdate ( JsonContext context ) {

        Log log = LogFactory.getLog( Dht.class );
        log.info( "contextUpdate called. " );
        
        try {
            reinitialize( true );
        } catch ( ServerFault fault ) {
            log.warn( "fault code=" + fault.getMessage() );
        }
    }

    private void setDhtProperty ( ) 
        throws ServerFault {

        Log log = LogFactory.getLog( Dht.class );

        if( jsonOptionDefMap.size() == 0 ) {
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }

        overlayMode = setString( (Map<String, Object>)jsonOptionDefMap
                                    .get("overlay-mode"), 
                                 overlayMode );

        if( !overlayMode.equals( "networked" ) ) {
            overlayMode = "local";
        }

        maxDrlSize = setInt( (Map<String, Object>)jsonOptionDefMap
                                .get("max-drl-size"), 
                             maxDrlSize );

        routingAlg = setString( (Map<String, Object>)jsonOptionDefMap
                                    .get("routing-algorithm"), 
                                 routingAlg );

        directoryType = setString( (Map<String, Object>)jsonOptionDefMap
                                        .get("directory-type"), 
                                   directoryType );

        workingDirectory = setString( (Map<String, Object>)jsonOptionDefMap
                                           .get("working-directory"), 
                                      workingDirectory );

        defaultTTL = setLong( (Map<String, Object>)jsonOptionDefMap
                                    .get("default-ttl"), 
                              defaultTTL ) * 60 * 60 * 1000;

        dhtPort = setString( (Map<String, Object>)jsonOptionDefMap
                                .get("dht-port"),  
                              dhtPort );

        bootServerList = setStringList( (Map<String, Object>)jsonOptionDefMap
                                            .get("boot-servers"), 
                                        bootServerList );

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

    public String getOverlayMode() {
        return this.overlayMode;
    }

    public DHT getDHT(){
        return this.proxyDht;
    }

    //--------------------------------------------------------------------------
   
    public void initialize() throws ServerFault {
        reinitialize( false );
    }

    public void reinitialize( boolean force ) 
        throws ServerFault {

        Log log = LogFactory.getLog( Dht.class );

        log.info( " dht initializing... " );

        readDhtContext();

        log.info( " boot servers=" + bootServerList);
        log.info( " overlayMode=" + overlayMode );
        log.info( " maxDrlSize=" + maxDrlSize );
        log.info( " routingAlg =" + routingAlg );
        log.info( " directoryType=" + directoryType );
        log.info( " workingDirectory=" + workingDirectory );
        log.info( " defaultTTL=" + defaultTTL );
        log.info( " dhtPort=" + dhtPort );

        String proxyHome = System.getProperty( "dip.proxy.home");
        log.info( " proxyHome=" + proxyHome );

        if( !workingDirectory.startsWith( File.separator )  
            && proxyHome != null && !proxyHome.isEmpty() ) {

            workingDirectory = proxyHome + File.separator + workingDirectory;
        }     

        log.info( "workingDir= " + workingDirectory );

        /*
        String proxyTime  = null;
        String proxyIdStr = null;
        BigInteger proxyIdSHA1 = null;
        ID proxyId = null;
        dhtProperties = new Properties();
        */

        /*
        if( !force ) {
            try{
                InputStream fis = new FileInputStream(propertiesFN); 
            
                dhtProperties.load( fis );
            
                dhtPort = dhtProperties.getProperty( "dht-port" );
            
                proxyHost = dhtProperties.getProperty("proxy-host");
                proxyIdStr = dhtProperties.getProperty("proxy-str-id");
                proxyIdSHA1 =  
                    new BigInteger( dhtProperties.getProperty( "proxy-sha1-id" ), 
                                    16 );
                proxyId = ID.getID( proxyIdSHA1, 20 );
                log.info( " old: proxyIdStr=" + proxyIdStr );
                log.info( " old:    proxyId=" + proxyIdSHA1.toString(16) );
            
            } catch (FileNotFoundException e ){
            
                writeRouterPropertyFile ( dhtPort );
        
            } catch (Exception uhe) {
                log.info( " unknown exception...");
            }

        } else {
            writeRouterPropertyFile ( dhtPort );
        }*/

        ID proxyId = writeRouterPropertyFile ( dhtPort );

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

            // if startup 

            /*
            if( proxyDht == null ){
                proxyDht = DHTFactory.getDHT( dhtc, proxyId );
            }*/
            if( force ) {
                 
                log.info( "before dht leaving... " );
                proxyDht.getRoutingService().leave();
                log.info( "after dht leaving and proxyDht=" + proxyDht );
                
                /* 
                log.info( "before dht stop... " );
                //proxyDht.clearRoutingTable();
                proxyDht.stop();
                log.info( "after dht stop and proxyDht=" + proxyDht );
                */
            }

            //proxyDht = DHTFactory.getDHT( dhtc, proxyId );
            //proxyDht = DHTFactory.getDHT( ++applicationID, ++applicationVersion, dhtc );

            /*
            if( force ){
                log.info( "dht before suspend. " );
                proxyDht.suspend();
                log.info( "dht before clear routing table. " );
                proxyDht.clearRoutingTable();
                log.info( "dht before resume. " );
                proxyDht.resume();
                log.info( "dht after resume. proxyDht=" + proxyDht );
            } */

            /*
            DHTConfiguration dhtcR = proxyDht.getConfiguration();
            dhtcR.setWorkingDirectory( "newDht" );
            log.info( "reset working directory. " );
            */
            
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
                
                proxyDht = DHTFactory.getDHT( (short)1, (short)1, dhtc );  

                log.info( "  local host=" +
                           localAddress.getHostAddress() );
                
                for( Iterator<String> i = bootServerList.iterator(); 
                     i.hasNext(); ) {
                
                    String bootHost = i.next();
                
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
            
            if( bootServerList.contains( localAddress.getHostAddress() )
                && ma == null ) {
               
                
                if( overlayMode.equalsIgnoreCase( "networked" ) ) { 
                    proxyDht = DHTFactory.getDHT( (short)1, (short)1, dhtc );
                } else {
                    log.info( "local to itself. " );
                    //proxyDht = DHTFactory.getDHT( ++applicationID, ++applicationVersion, dhtc );
                    proxyDht = DHTFactory.getDHT( (short)2, (short)2, dhtc );
                }

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

    /* 
    public void initialize() throws ServerFault {
        Log log = LogFactory.getLog( Dht.class );

        log.info( " dht initializing... " );

        readDhtContext();

        log.info( " boot servers=" + bootServerList);
        log.info( " overlayMode=" + overlayMode );
        log.info( " maxDrlSize=" + maxDrlSize );
        log.info( " routingAlg =" + routingAlg );
        log.info( " directoryType=" + directoryType );
        log.info( " workingDirectory=" + workingDirectory );
        log.info( " defaultTTL=" + defaultTTL );
        log.info( " dhtPort=" + dhtPort );

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
            InputStream fis = new FileInputStream(propertiesFN); 
            
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
                    new FileOutputStream( propertiesFN );
                
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
            
            //dhtc.setDoReputOnReplicas( true );

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
                
                for( Iterator<String> i = bootServerList.iterator(); 
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
            
            if( bootServerList.contains( localAddress.getHostAddress() )
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
    */ 
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
        
        //log.info( " got value info set ..." );

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

        //log.info( "updateItem entering with newItem=" + newItem );

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

                //log.info( "   item=" + proxyItem.toString() );
                
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
                //log.info( "updateItem: newFlag=true: add newItem=" + newItem );
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

            //log.info( "updateItem: after put: dht rc=" + proxyDht.get(rid) );

        } catch ( Exception e ) {
            log.info( "dht exception:" + e.toString() );
        }

        //log.info( "update(UPDATE): done" );
    }

    public void deleteItem( ID rid, DhtRouterItem newItem ) {
        
        Log log = LogFactory.getLog(Dht.class);

        DhtRouterList drl = getDhtRouterList( rid );
                 
        boolean removeFlag = false;
 
        if( drl != null ) {
            for( int i = 0; i < drl.size(); i++ ) {
                    
                DhtRouterItem proxyItem = drl.getItem( i );                   
                //log.info( "   item=" + proxyItem.toString() );
                    
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

        //log.info( "update(DELETE): done" );
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


    private String setString( Map defs, String defaultValue) {

        if( defs.get("value") != null 
            && ((String)defs.get("type")).equalsIgnoreCase("string" ) ) {

            return (String) defs.get("value");
        } 
        return defaultValue;
    }
    
    private int setInt(Map<String,Object> defs, int defaultValue ) {

        if( defs.get("value") != null
            && ((String)defs.get("type")).equalsIgnoreCase("string" ) ){
            
            return Integer.parseInt( (String) defs.get("value") );
        }

        return defaultValue;

    }

    private long setLong(Map<String,Object> defs, long defaultValue){

        if( defs.get("value") != null
            && ((String)defs.get("type")).equalsIgnoreCase("string" ) ){

            return Long.parseLong( (String) defs.get("value") );
        }

        return defaultValue;
    }

    private List<String> setStringList( Map<String,Object> defs, 
                                        List<String> defaultValue ){

        if( defs.get("value") != null
            && ((String)defs.get("type")).equalsIgnoreCase("string-list" ) ) {

            return (List<String>)defs.get("value");
        }

        return defaultValue;
    }

    private ID writeRouterPropertyFile ( String dhtPort ) throws ServerFault {

        Log log = LogFactory.getLog( Dht.class );
        dhtProperties = new Properties();

        if( proxyHost == null ) {
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                proxyHost = localHost.getHostAddress();
                
            } catch (UnknownHostException ue) {
                log.info( " unknown host...");
                proxyHost = "dip-proxy" ;
            }
        }     
            
        Calendar now = Calendar.getInstance();
        String proxyTime = Long.toString( now.getTimeInMillis() );
            
        String proxyIdStr = proxyHost + ":" + dhtPort + ":" + proxyTime;
        
        ID proxyId = ID.getSHA1BasedID( proxyIdStr.getBytes() );
            
        BigInteger proxyIdSHA1 = proxyId.toBigInteger();
            
        log.info( " new: proxyIdStr=" + proxyIdStr );
        log.info( " new:    proxyId=" + proxyIdSHA1.toString(16) );
       
        dhtProperties.setProperty( "dht-port", dhtPort );     
        dhtProperties.setProperty( "proxy-host", proxyHost );            
        dhtProperties.setProperty( "proxy-str-id", proxyIdStr );
        dhtProperties.setProperty( "proxy-sha1-id", 
                                   proxyIdSHA1.toString(16) );


        try {
            File propertiesFile = new File( propertiesFN );

            OutputStream fos =
                new FileOutputStream( propertiesFile );
                
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
        } catch ( SecurityException se ) {
            log.info( "  dhtrouter.properties: " +
                      "cannot delete(SecurityException)" );

        }

        return proxyId;
    }
}
