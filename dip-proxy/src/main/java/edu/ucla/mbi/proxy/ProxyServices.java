package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/trunk/dip-proxy/src/#$
 * $Id$
 * Version: $Rev$
 *===========================================================================
 *
 * ProxyServices:
 *
 *========================================================================= */

import edu.ucla.mbi.dxf14.*;
                                                                           
public interface ProxyServices{
    //public void setNativeService(WebServices webServices);
    //public void setTransformer(ProxyTransformer proxyTransformer);
  
    public edu.ucla.mbi.dxf14.DatasetType 
	get(String ac, String ns, String operation, String detail, 
	    String serverColumn) 
	throws Exception;
}

