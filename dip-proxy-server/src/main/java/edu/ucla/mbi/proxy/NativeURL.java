package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NativeURL
 *
 *    returns string representation of a data record requested from the
 *    server using ns/ac (namespace/accession) pair as identifier and
 *    operation as the remote service name
 *
 *=========================================================================== */



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.net.*;
import java.io.*;

import edu.ucla.mbi.fault.*;

public class NativeURL {
                                                                            
    public static String query( String url, int timeOut ) throws ProxyFault {
        Log log =  LogFactory.getLog( NativeURL.class );	
        String retVal = "";
	
        try {
	        java.net.URL xmlURL = new URL( url );
	        java.net.URLConnection conn = xmlURL.openConnection();

	        // setting timeout ensure the client does not deadlock indefinitely
	        // -----------------------------------------------------------------
	        conn.setConnectTimeout( timeOut ); // uTimeout is int as milliseconds
	        conn.setReadTimeout( timeOut );
	    
            BufferedReader in = 
		        new BufferedReader( new InputStreamReader( conn.getInputStream() ) );

            if ( in != null ){
		        String inputLine = new String();
		        StringBuffer sb = new StringBuffer();
		        while( ( inputLine = in.readLine() ) != null ) {
		            sb.append(inputLine);
		        }
		        retVal = sb.toString();
	        }
        } catch ( Exception e ) {
	        log.info( "NativeURL: exception: " + e.toString());
            if( e.toString().contains( "TimeoutException" ) ) {
		        throw FaultFactory.newInstance( Fault.REMOTE_TIMEOUT );  // timeout
	        }else {
                //*** including http status 503 Service Temporarily Unavailable
		        throw FaultFactory.newInstance( Fault.REMOTE_FAULT );  // unknown remote
            }
        }

        if( retVal == null ) {
            log.info( "query:  return null. " );
            throw FaultFactory.newInstance( Fault.NO_RECORD ); // no hits
        } else {        
	        return retVal;
        }
    }
}
