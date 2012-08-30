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

        restUrl = restUrl.replaceAll( restAcTag, ac );
        try {
            retVal = NativeURL.query( restUrl, timeOut );
        } catch( ProxyFault fault ) {
            throw fault;
        }

        if( retVal.endsWith( "</cause></error></ResultSet>" ) ) {
            log.warn( "query: return error=" + retVal );
            throw FaultFactory.newInstance( Fault.REMOTE_FAULT );
        }

        NativeRecord record = new NativeRecord( provider, service, ns, ac );
        record.setNativeXml( retVal );
        return record;

    }
}
