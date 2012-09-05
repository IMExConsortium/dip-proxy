package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/main$
 * $Id:: NcbiServer.java 2607 2012-07-31 20:38:53Z wyu                         $
 * Version: $Rev:: 2607                                                        $
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

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;

public class NativeRestServer extends RemoteNativeServer {

    private Log log = LogFactory.getLog( NativeRestServer.class );

    private String restUrl = null;
    private String restAcTag = null;
   
    public void setRestUrl( String url ) {
        url = url.replaceAll("^\\s+","");
        url = url.replaceAll("\\s+$","");

        this.restUrl=url;
    }

    public String getRestUrl(){
        return restUrl;
    }

    public void setRestAcTag( String tag ) {
        tag = tag.replaceAll("^\\s+","");
        tag = tag.replaceAll("\\s+$","");

        this.restAcTag = tag ;
    }

    public String getRestAcTag(){
        return restAcTag;
    }
 
    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeOut ) throws ProxyFault {

        String retVal = null;
        log.info( "NcbiServer: NS=" + ns + " AC=" + ac + " OP=" + service );

        String real_restUrl = restUrl.replaceAll( restAcTag, ac );
        try {
            retVal = query( real_restUrl, timeOut );
        } catch( ProxyFault fault ) {
            throw fault;
        }

        if( retVal.endsWith( "</cause></error></ResultSet>" ) ) {
            //*** this error for intermine server
            log.warn( "getNative: return error=" + retVal );
            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        if( retVal.endsWith( "<ResultSet ></ResultSet>" ) ) {
            //**** this fault for intermine server
            log.warn( "getNative: return an empty set. " );
            throw FaultFactory.newInstance( Fault.NO_RECORD );
        }

        NativeRecord record = new NativeRecord( provider, service, ns, ac );
        record.setNativeXml( retVal );
        return record;

    }

    private String query( String url, int timeOut ) throws ProxyFault {
        String retVal = "";

        try {
            java.net.URL xmlURL = new URL( url );
            java.net.HttpURLConnection conn
                    = (HttpURLConnection) xmlURL.openConnection();

            if( conn.getResponseCode() != 200 ) {
                log.warn ( "query: connectin get response message: "
                           + conn.getResponseMessage() );
                throw new IOException( conn.getResponseMessage() );
            }

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

            in.close();
            conn.disconnect();

        } catch ( Exception e ) {
            log.info( "query: exception: " + e.toString());
            if( e.toString().contains( "TimeoutException" )
                    || e.toString().contains( "Read timeout" ) )
            {
                throw FaultFactory.newInstance( Fault.REMOTE_TIMEOUT );  // timeout
            } else if ( e.toString().contains( "No result found" ) ) {
                throw FaultFactory.newInstance( Fault.NO_RECORD );
            } else {
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
