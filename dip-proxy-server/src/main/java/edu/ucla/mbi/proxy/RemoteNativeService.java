package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * RemoteNativeServices:
 *  returns remote native record;
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.fault.*;

class RemoteNativeService { 

    private Log log = LogFactory.getLog( RemoteNativeService.class );

    private WSContext wsContext;
    
    public RemoteNativeService() { }
    
    public void setWsContext( WSContext context ) {
        this.wsContext = context;
    }

    //--------------------------------------------------------------------------

    public NativeRecord getNativeFromRemote ( String provider, 
                                              String service, 
                                              String ns, 
                                              String ac 
                                              ) throws ServerFault {

        int retry = wsContext.getMaxRetry( provider );
        NativeRecord remoteRecord = null;
        ServerFault retryFault = null;
        
        while ( retry > 0 && remoteRecord == null ) {    

            boolean deleteFlag = false;
            
            NativeServer nativeServer = null;                
            retry--;

            if( retryFault != null ) {
                retryFault = null;
            }

            log.info( "getNativeFromRemote: retry left=" + retry );
            log.info( "getNativeFromRemote: before selectNextRemoteServer. " );    

            
            nativeServer = wsContext.getNativeServer( provider );
            
            try {                                                       
                remoteRecord = nativeServer
                    .getNativeRecord( provider, service, ns,
                                      ac, wsContext.getTimeout( provider ) );

            } catch( ServerFault fault ) {
                log.warn( "getNativeFromRemote: RemoteServer getNative() " + 
                          "fault for ac=" + ac + ": " + fault.getMessage()); 
                retryFault = fault;
                deleteFlag = true;
                
            } catch ( Exception ex ) {
                log.warn( "getNativeFromRemote: ex=" + ex.toString() );
                deleteFlag = true;
                retryFault = ServerFaultFactory.newInstance( Fault.UNKNOWN );
            }
            
            log.info( "getNativeFromRemote: after got remoteRecord=" + 
                      remoteRecord );
            
            if( remoteRecord != null && !isRecordValid( remoteRecord ) ) {

                deleteFlag = true;
                retryFault = ServerFaultFactory
                    .newInstance( Fault.VALIDATION_ERROR );
                remoteRecord = null;
            }            
        }

        if( remoteRecord == null ) {
            if( retryFault != null ) {
                throw retryFault;
            } 
            return null;
         }

        log.info( "valid record="+ remoteRecord);
        
        remoteRecord.resetExpireTime( wsContext.getTtl( provider ) );
        return remoteRecord;
    }

    private boolean isRecordValid( NativeRecord record ) {

        if( record.getNativeXml() == null 
            || record.getNativeXml().isEmpty() ) {

            return false;
        }
        return true;
    }
    
}
