package edu.ucla.mbi.proxy.intermine;

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

public class InterMineServer extends RemoteNativeServer {

    private Log log = LogFactory.getLog( InterMineServer.class );
    private String interMineURL = null;
    private String interMineAcTag = null;
    
    public void initialize() {
        if( getContext() != null ){

            interMineURL = (String) getContext().get( "interMineURL" );
            interMineAcTag = (String) getContext().get( "interMineAcTag" );

            if( interMineURL != null &&  interMineURL.length() > 0
                    && interMineAcTag != null && interMineAcTag.length() > 0 ) 
            {
                interMineURL = interMineURL.replaceAll( "^\\s+", "" );
                interMineURL = interMineURL.replaceAll( "\\s+$", "" );
                interMineAcTag = interMineAcTag.replaceAll( "^\\s+", "" );
                interMineAcTag = interMineAcTag.replaceAll( "\\s+$", "" );
            } else {
                log.warn( "InterMineServer: InterMineServer initializing failed "
                           + "because interMineURL or interMineAcTag is not set. " );
                return;
            }
        }
    }
       
    public NativeRecord getNative( String provider, String service, String ns,
            String ac, int timeOut ) throws ProxyFault {

        String retVal = null;
        log.info( "NcbiServer: NS=" + ns + " AC=" + ac + " OP=" + service );

        if ( service.contains( "mine" ) ) {
            interMineURL = interMineURL.replaceAll( interMineAcTag, ac );
            try {
                retVal = NativeURL.query( interMineURL, timeOut );
            } catch( ProxyFault fault ) {
                throw fault;
            }
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
