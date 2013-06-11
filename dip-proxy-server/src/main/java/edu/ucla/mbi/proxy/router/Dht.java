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

    private short networked_app_id = 1;
    private short local_app_id = 2;

    public Dht() { }

    private JsonContext dhtContext;

    public void setDhtContext( JsonContext context ){
        this.dhtContext = context;
    }
  
    public String getDhtContextString () {
        Log log = LogFactory.getLog( Dht.class );
        log.info( "getDhtContextString... " );

        if( dhtContext == null ) return null;

        if( dhtContext.getJsonConfigString() == null ) {
            try {
                dhtContext = readDhtContext();
            } catch ( ServerFault fault ) {
                log.info( "readDhtContext got fault. " );
                return null;
            }
        }
        return dhtContext.getJsonConfigString();    
    } 

    private JsonContext readDhtContext() throws ServerFault {
        Log log = LogFactory.getLog( Dht.class );
        log.info( "readDhtContext... " );

        FileResource fr = (FileResource) dhtContext
                                .getConfig().get("json-source");

        if ( fr == null ) return null;

        try {
            dhtContext.readJsonConfigDef( fr.getInputStream() );
        } catch ( Exception e ){
            log.warn( "initialize exception: " + e.toString() );
            throw ServerFaultFactory.newInstance ( Fault.JSON_CONFIGURATION );
        }

        return dhtContext;
    }

    private void extractDhtContext() throws ServerFault {
        
        Log log = LogFactory.getLog( Dht.class );
        log.info( "extractDhtContext... " );
        
        /*
        FileResource fr = (FileResource) dhtContext
                                .getConfig().get("json-source");

        if ( fr == null ) return;
        
        try {
            dhtContext.readJsonConfigDef( fr.getInputStream() );
        } catch ( Exception e ){i
            log.warn( "initialize exception: " + e.toString() );
            throw ServerFaultFactory.newInstance ( Fault.JSON_CONFIGURATION );
        }
        */

        dhtContext = readDhtContext();

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

        log.info( "before get networked-app-id." );
        networked_app_id = setShort ( (Map<String, Object>)jsonOptionDefMap
                                          .get("networked-application-id"),
                                      networked_app_id );
 
        log.info( "networked_app_id= " + networked_app_id );

        local_app_id = setShort ( (Map<String, Object>)jsonOptionDefMap
                                      .get("local-application-id"),
                                  local_app_id );

        log.info( "local_app_id= " + local_app_id );
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
        Log log = LogFactory.getLog( Dht.class );
        log.info( "dht initializing... " );

        reinitialize( false );
    }

    public void reinitialize( boolean force ) 
        throws ServerFault {

        Log log = LogFactory.getLog( Dht.class );

        log.info( " dht reinitializing... " );

        //readDhtContext();
        extractDhtContext();

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

                proxyDht = DHTFactory.getDHT( networked_app_id, 
                    networked_app_id, dhtc, proxyId );
                
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
                    proxyDht = DHTFactory.getDHT( networked_app_id, 
                        networked_app_id, dhtc, proxyId );
                } else {
                    log.info( "local to itself. " );
                    
                    proxyDht = DHTFactory.getDHT( local_app_id, 
                        local_app_id, dhtc, proxyId );
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
   
    public DhtRouterList getDhtRouterList( ID rid ) {

        Log log = LogFactory.getLog( Dht.class );
        Set<ValueInfo<DhtRouterList>> vis = null;

        try {
            vis = proxyDht.get(rid);
        } catch( RoutingException re ) {
            log.warn( "Fault: routing exception: " + re.toString() );
            return null;
        }
        
        if( vis == null || vis.size() == 0 ) return null;

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

        } catch ( Exception e ) {
            log.info( "dht exception:" + e.toString() );
        }
    }

    public void deleteItem( ID rid, DhtRouterItem newItem ) {
        
        Log log = LogFactory.getLog(Dht.class);

        DhtRouterList drl = getDhtRouterList( rid );
                 
        boolean removeFlag = false;
 
        if( drl != null ) {
            for( int i = 0; i < drl.size(); i++ ) {
                    
                DhtRouterItem proxyItem = drl.getItem( i );                   
                    
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

    private String setString( Map defs, String defaultValue) {

        if( defs != null && defs.get("value") != null 
            && ((String)defs.get("type")).equalsIgnoreCase("string" ) ) {

            return (String) defs.get("value");
        } 
        return defaultValue;
    }
    
    private int setInt(Map<String,Object> defs, int defaultValue ) {

        if( defs != null && defs.get("value") != null
            && ((String)defs.get("type")).equalsIgnoreCase("string" ) ){
            
            return Integer.parseInt( (String) defs.get("value") );
        }
        return defaultValue;
    }

    private short setShort(Map<String,Object> defs, short defaultValue ) {

        if( defs != null && defs.get("value") != null
            && ((String)defs.get("type")).equalsIgnoreCase("string" ) ){

            return Short.parseShort( (String) defs.get("value") );
        }
        return defaultValue;
    }

    private long setLong(Map<String,Object> defs, long defaultValue){

        if( defs != null && defs.get("value") != null
            && ((String)defs.get("type")).equalsIgnoreCase("string" ) ){

            return Long.parseLong( (String) defs.get("value") );
        }
        return defaultValue;
    }

    private List<String> setStringList( Map<String,Object> defs, 
                                        List<String> defaultValue ){

        if( defs != null && defs.get("value") != null
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
