package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/main$
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NCBIServer:
 *    services provided by NCBI web services
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.*;
import java.io.*;
import java.util.*;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.util.context.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.xpath.*;
import javax.xml.parsers.*;
import java.net.URL;

public class NativeRestServer implements NativeServer, DomServer { 
    private Log log = LogFactory.getLog( NativeRestServer.class );
    
    private RestServer restServer = null;
    
    public void setRestServer( RestServer server ) {
        restServer = server;
    }

    public void initialize() throws ServerFault {

        log.info( "initialize starting... " );
    }


    public Document getNativeDom( String provider, String service,
                                  String ns, String ac
                                  ) throws ServerFault {
        
        return restServer.getNativeDom( provider, service, ns, ac);
    }
    
    public NativeRecord getNativeRecord( String provider, String service, 
        String ns, String ac, int timeout ) throws ServerFault {

        log.info( "getNativeRecord: PROVIDER=" + provider + " and SERVICE=" +
                  service + " and NS=" + ns + " AC=" + ac );

        String retVal = 
            restServer.getNativeString( provider, service, ns, ac, timeout );

        if( retVal.endsWith( "</cause></error></ResultSet>" ) ) {
            // *** this error for intermine server
            log.warn( "getNativeRecord: return error=" + retVal );
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        if( retVal.endsWith( "<ResultSet ></ResultSet>" ) ) {
            // **** this fault for intermine server
            log.warn( "getNativeRecord: return an empty set. " );
            throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
        }

        if( retVal.endsWith( "<TaxaSet></TaxaSet>" ) ) {
             // **** this fault for NCBI taxon server
            log.warn( "getNativeRecord: return an empty set. " );
            throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
        }

        if( retVal.contains("<INSDSet><Error>")
                    || retVal.contains( "<TSeqSet/>" ) ) {

            //*** this fault for NCBI refseq
            log.warn( "getNativeRecord: refseq get wrong retVal for ac " + ac + "." );
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        //*** this fault for MBI prolinks
        if( retVal.contains( "<faultCode>97</faultCode>" ) ) {
            log.info( "getNativeRecord: return faultCode 97. " );
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        } else if ( retVal.contains( "<faultCode>5</faultCode>" ) ) {
            log.info( "getNativeRecord: return faultCode 5. " );
            throw ServerFaultFactory.newInstance( Fault.NO_RECORD );
        } else if ( retVal.contains( "</faultCode>") ) {
            log.warn( "getNativeRecord: return faultCode=" + retVal );
            throw ServerFaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        NativeRecord record = new NativeRecord( provider, service, ns, ac );
        record.setNativeXml( retVal );
        return record;
    }
}
