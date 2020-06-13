package edu.ucla.mbi.proxy.ebi;

/*==============================================================================
 *
 * EbiServer:
 *    services provided by EBI web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URL;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.bind.*;
import javax.xml.ws.BindingProvider;
//import com.sun.xml.ws.developer.JAXWSProperties;

import javax.xml.namespace.QName;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;

public class EbiServer implements NativeServer {

    private List<String> searchDB;

    /*
     * generate static instance of JAXBContext see
     * https://jaxb.dev.java.net/faq/index.html#threadSafety for details.
     */

    //static final JAXBContext acrContext = initAcrContext();

    private NativeServer nativeRestServer = null;
    private Map<String, Object> context = null; 
     
    //--------------------------------------------------------------------------

    public void setContext( Map<String,Object> context ) {
        this.context = context;
    }

    //--------------------------------------------------------------------------
    /**
    private static JAXBContext initAcrContext() {

        Log log = LogFactory.getLog( EbiServer.class );
        try {
            log.info( " JAXBContext.initAcrContext() called" );
            JAXBContext jbx =
                    JAXBContext.newInstance( "uk.ac.ebi.picr", EbiServer.class
                            .getClassLoader() );
            return jbx;
        } catch ( JAXBException jbe ) {
            log.info( "JAXBContext.initAcrContext(): " + jbe.toString() );
        }
        return null;
    }

    public static JAXBContext getAcrContext() {
        return acrContext;
    }
    **/
    //--------------------------------------------------------------------------
    
    public void initialize() throws ServerFault {
        Log log = LogFactory.getLog( EbiServer.class );
        log.info( "initializing: " );

        searchDB = new ArrayList<String>();

        if(  context == null ) {
            log.warn( "EbiServer: initializing failed " + 
                      "because context is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
 
        nativeRestServer = (NativeServer) context.get( "nativeRestServer" );

        if( nativeRestServer == null ) {
            log.warn( "EbiServer: initializing failed " +
                      "because nativeRestServer is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }


        if( context.get( "searchDbList" ) != null ) {
            log.info( " searchDB list: " );
            for ( Iterator ii = 
                      ((List) context.get( "searchDbList" )).iterator(); 
                  ii.hasNext(); ) {
                
                String db = (String) ii.next();
                db = db.replaceAll( "\\s+", "" );
                log.info( "  db=" + db );
                searchDB.add( db );
            }            
        } else {
            log.warn( "EbiServer: initializing failed " +
                      "because searchDbList is null. " );
            throw ServerFaultFactory.newInstance( Fault.JSON_CONFIGURATION );
        }
        
    }

    //-------------------------------------------------------------------------
    
    public NativeRecord getNativeRecord( String provider, String service, 
                                         String ns, String ac, int timeout  
                                         ) throws ServerFault { 
    
        Log log = LogFactory.getLog( EbiServer.class );
        log.info( "NS=" + ns + " AC=" + ac + " SRV=" + service );

        return nativeRestServer.getNativeRecord( provider, service, 
                                                 ns, ac, timeout );
        
    }     
}
