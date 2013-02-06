package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/src/mai#$
 * $Id:: NcbiServer.java 2607 2012-07-31 20:38:53Z wyu                         $
 * Version: $Rev:: 2607                                                        $
 *==============================================================================
 *
 * NativeServer:
 *    An interface is of one function getNative(...)
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.*;
import java.io.*;
import java.util.Map;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

import edu.ucla.mbi.fault.*;

public interface NativeServer {

    public NativeRecord getNative( String provider, String service, 
                                   String ns, String ac, int timeout 
                                   ) throws ProxyFault; 

    //public void setContext( Map<String,Object> context );

    //public void initialize() throws ProxyFault;
    
}
