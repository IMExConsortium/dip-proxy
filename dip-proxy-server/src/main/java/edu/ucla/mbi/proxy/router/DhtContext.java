package edu.ucla.mbi.proxy.router;

/*==============================================================================
 * $HeadURL:: https://imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-proxy-s#$
 * $Id:: Dht.java 3240 2013-06-18 18:31:47Z wyu                                $
 * Version: $Rev:: 3240                                                        $
 *==============================================================================
 *
 * DhtContex:
 *   DHT configuration
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.context.*;

public class DhtContext {

    Log log = LogFactory.getLog( DhtContext.class );
   
    private Map<String, Object> jsonOptionDefMap = new HashMap();
 
    public DhtContext() { }

    private JsonContext jsonContext;

    public void setJsonContext( JsonContext context ){
        this.jsonContext = context;
    }

    public String getJsonFilePath() {
        if( jsonContext == null ) return null;
        return (String)jsonContext.getConfig().get( "json-config" );
    }
    
    public void writeToJsonFile( String realPath ) throws Exception {

        File sf = new File( realPath );
        try {
            PrintWriter spw = new PrintWriter( sf );
            jsonContext.writeJsonConfigDef( spw );
            spw.close();
        } catch ( Exception ex ) {
            throw ex;
        }
    }

    public String getDhtContextString () {
        log.info( "getDhtContextString... " );

        if( jsonContext == null ) return null;

        if( jsonContext.getJsonConfigString() == null ) {
            try {
                jsonContext = readDhtContext();
            } catch ( ServerFault fault ) {
                log.info( "readDhtContext got fault. " );
                return null;
            }
        }
        return jsonContext.getJsonConfigString();    
    } 

    private JsonContext readDhtContext() throws ServerFault {
        log.info( "readDhtContext:readDhtContext... " );

        FileResource fr = (FileResource) jsonContext
                                .getConfig().get("json-source");

        if ( fr == null ) return null;

        try {
            jsonContext.readJsonConfigDef( fr.getInputStream() );
        } catch ( Exception e ){
            log.warn( "initialize exception: " + e.toString() );
            throw ServerFaultFactory.newInstance ( Fault.JSON_CONFIGURATION );
        }

        return jsonContext;
    }

    public void setDhtOption( String oppName, String optionDefValue )
        throws ServerFault {

        log.info( "setDhtOption: setting option... " );
        
        Map<String, Object> dhtJsonMap = jsonContext.getJsonConfig();

        if ( dhtJsonMap.get( "option-def" ) != null ) {
            recursiveOptionDef ( dhtJsonMap, oppName, optionDefValue );
        } else {
            throw ServerFaultFactory
                .newInstance( Fault.JSON_CONFIGURATION );
        }
    }

    public void initialize() throws ServerFault {
        
        log.info( "extractDhtContext... " );
        
        jsonContext = readDhtContext();

        log.info( "initialize: after readDhtContext. " );
        Map<String, Object> dhtJsonMap = jsonContext.getJsonConfig();

        if ( dhtJsonMap.get( "option-def" ) != null ) {        
            recursiveOptionDef ( dhtJsonMap, null, null );
        } else {
            throw ServerFaultFactory
                .newInstance( Fault.JSON_CONFIGURATION );
        }
        
        log.info( "after exractDhtContext. " );
    }
     
    
    private void recursiveOptionDef( Map<String, Object> jsonMap,
                                    String oppName,
                                    String optionDefValue ) {

        Map<String,Object> optionDef =
            (Map<String,Object>) jsonMap.get( "option-def" );

        Set<String> newDefs = optionDef.keySet();

        for( Iterator<String> is = newDefs.iterator(); is.hasNext(); ){

            String key = is.next();

            Map<String, Object> def =
                (Map<String, Object>)optionDef.get( key );

            if( def.get( "value" ) != null ) {
                if( oppName != null && def.get( "opp") != null ) {
                    //*** setDhtOption
                    if( def.get( "opp" ).equals( oppName ) ) {
                        def.put( "value", optionDefValue );
                        return;
                    }
                } else {
                    //*** extractDhtContext
                    jsonOptionDefMap.put( key, def );
                }
            }

            if( def.get( "option-def" ) != null ){
                recursiveOptionDef( def, oppName, optionDefValue );
            }
        }
    }

    //--------------------------------------------------------------------------
    
    public String getString( String name, String defVal ) throws ServerFault{
       
        if( jsonOptionDefMap == null)
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );

        if( jsonOptionDefMap.containsKey ( name ) ) {
            
            Map<String, Object> def = 
                (Map<String, Object>)jsonOptionDefMap.get( name );
 
            if( def != null && def.get("value") != null
                && ((String)def.get("type")).equalsIgnoreCase("string" ) ) {

                return (String) def.get("value");
            }
        }
        return defVal;
    }
   
    public int getInt( String name, int defVal ) throws ServerFault{

        if( jsonOptionDefMap == null)
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );

        if( jsonOptionDefMap.containsKey ( name ) ) { 

            Map<String, Object> def = 
                (Map<String, Object>)jsonOptionDefMap.get( name );
            
            if( def != null && def.get("value") != null
                && ((String)def.get("type")).equalsIgnoreCase("string" ) ) {

                return Integer.parseInt( (String) def.get("value") );
            }
        }
        return defVal;
    }

    public short getShort( String name, short defVal ) throws ServerFault{

        if( jsonOptionDefMap == null)
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        
        if( jsonOptionDefMap.containsKey ( name ) ) {
            
            Map<String, Object> def =
                (Map<String, Object>)jsonOptionDefMap.get( name );

            if( def != null && def.get("value") != null
                && ((String)def.get("type")).equalsIgnoreCase("string" ) ) {

                return Short.parseShort( (String) def.get("value") );
            }
        }
        return defVal;
    }

    public long getLong( String name, long defVal ) throws ServerFault{

        if( jsonOptionDefMap == null )
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
            
        if( jsonOptionDefMap.containsKey ( name ) ) {

            Map<String, Object> def =
                (Map<String, Object>)jsonOptionDefMap.get( name );
            
            if( def != null && def.get("value") != null
                && ((String)def.get("type")).equalsIgnoreCase("string" ) ) {

                return Long.parseLong( (String) def.get("value") );
            }
        }
        return defVal;
    }

    public List<String> getStringList ( String name, List<String> defVal ) 
        throws ServerFault{

        if( jsonOptionDefMap == null )
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );

        if( jsonOptionDefMap.containsKey ( name ) ) {

            Map<String, Object> def =
                (Map<String, Object>)jsonOptionDefMap.get( name );

            if( def != null && def.get("value") != null
                && ((String)def.get("type")).equalsIgnoreCase("string-list" ) ) {

                return (List<String>) def.get("value") ;
            }
        }
        return defVal;
    }

    public boolean getBoolean( String name, boolean flag ) 
        throws ServerFault{
        
        if( jsonOptionDefMap == null )
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );

        if( jsonOptionDefMap.containsKey ( name ) ) {

            Map<String, Object> def =
                (Map<String, Object>)jsonOptionDefMap.get( name );

            if( def != null && def.get("value") != null
                && ((String)def.get("type")).equalsIgnoreCase("boolean" ) ) {

                String value = (String) def.get("value");

                if( value.equals( "true" ) ) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return flag;
    }
}
