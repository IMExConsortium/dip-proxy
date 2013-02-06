package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * RemoteServer:
 *
 *    An interface with two functions:
 *          getNative(...) and buildDxf(...)
 *    
 *=========================================================================== */

import edu.ucla.mbi.dxf14.DatasetType;
import edu.ucla.mbi.cache.NativeRecord;

public interface RemoteServer extends NativeServer{
    
    public NativeRecord getNative( String provider, String service,
                                   String ns, String ac, int timeout, 
                                   int retry ) throws ProxyFault;
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
                                 String detail, String provider, String service 
                                 ) throws ProxyFault;
    
    
    public boolean isNative();
    public String getAddress();
    
}

