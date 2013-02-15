package edu.ucla.mbi.proxy.router;

/*===========================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/service#$
 * $Id:: CachingService.java 130 2009-02-03 17:58:49Z wyu                   $
 * Version: $Rev:: 130                                                      $
 *===========================================================================
 *
 * Router:
 *   selects remore server to call 
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

import edu.ucla.mbi.server.RemoteServerContext;
import edu.ucla.mbi.proxy.RemoteServer;

import edu.ucla.mbi.proxy.NativeServer;

public interface Router{ //  extends Observer {

    public Router createRouter();
    
    public void setRemoteServerContext( RemoteServerContext rsc );
    public RemoteServerContext getRemoteServerContext();
    
    public NativeServer getNativeServer( String service );

    public NativeServer getLastProxyServer( String service );

    public NativeServer getNextProxyServer( String service );



    public void setMaxRetry( int retry );
    public int getMaxRetry();


    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------

    public NativeServer getNextProxyServer( String provider,
                                            String service,
                                            String namespace, 
                                            String accession );
    //,String operation );

    public void update( Object observer, Object arg );
  
}
