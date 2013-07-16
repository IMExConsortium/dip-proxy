package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL:: https://wyu@imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-pro#$
 * $Id:: DomServer.java 3317 2013-07-11 00:07:20Z lukasz                       $
 * Version: $Rev:: 3317                                                        $
 *==============================================================================
 *
 * DomServer:
 *    An interface is of one function getNativeDom(...)
 *
 *=========================================================================== */

import org.w3c.dom.Document;
import edu.ucla.mbi.fault.*;

public interface DomServer {

    public Document getNativeDom( String provider, String service,
                                  String ns, String ac
                                  ) throws ServerFault;
}
