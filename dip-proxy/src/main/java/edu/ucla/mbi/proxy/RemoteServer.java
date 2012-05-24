package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * RemoteServer:
 *
 *    returns string representation of a data record requested from the 
 *    server using ns/ac (namespace/accession) pair as identifier and
 *    operation as the remote service name
 *
 *========================================================================= */

import edu.ucla.mbi.services.ServiceException;
import edu.ucla.mbi.dxf14.DatasetType;
import edu.ucla.mbi.cache.NativeRecord;

public interface RemoteServer{
    
    public NativeRecord getNative( String provider, String service,
                             String ns, String ac, int timeOut ) 
        throws ServiceException;
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
				 String detail, String service, 
				 ProxyTransformer pTrans ) 
        throws ServiceException;
    

    public boolean isNative();
    public String getAddress();
    
}

