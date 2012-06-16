package edu.ucla.mbi.proxy;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * ProxyServices:
 *
 *========================================================================= */

import edu.ucla.mbi.dxf14.*;
                                                                           
public interface ProxyServices {
  
    public edu.ucla.mbi.dxf14.DatasetType 
	    get(String ac, String ns, String operation, String detail, 
	        String serverColumn) throws Exception;
}

