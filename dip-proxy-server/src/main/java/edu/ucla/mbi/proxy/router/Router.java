package edu.ucla.mbi.proxy.router;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * Router:
 *   finds next remote server to call 
 *
 *=========================================================================== */

import edu.ucla.mbi.proxy.context.RemoteServerContext;

import edu.ucla.mbi.proxy.NativeServer;

public interface Router{
    
    public Router createRouter();
    
    public void setRemoteServerContext( RemoteServerContext rsc );
    public RemoteServerContext getRemoteServerContext();
    
    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------

    public NativeServer getNextProxyServer( String provider,
                                            String service,
                                            String namespace, 
                                            String accession );
    
    public void update( Object observer, Object arg );
    
}
