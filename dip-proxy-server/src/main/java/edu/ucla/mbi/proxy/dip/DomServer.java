package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
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
