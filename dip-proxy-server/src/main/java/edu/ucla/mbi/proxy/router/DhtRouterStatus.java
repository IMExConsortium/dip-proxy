package edu.ucla.mbi.proxy.router;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * DhtRouterStatus:
 *   returns dht router status
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ow.id.*;
import ow.dht.*;
import ow.messaging.*;
import ow.routing.*;

import java.net.*;
import java.io.*;
import java.util.*;
import java.math.BigInteger;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;

public class DhtRouterStatus {

    private DhtRouter dhtRouter = null;

    public void setDhtRouter( DhtRouter router ){
        this.dhtRouter = router;
    }
    
    public DhtRouter getDhtRouter(){
        return dhtRouter;
    }
    
    public List<DhtRouterList> getDhtRouterList( String provider,
                                                 String service,
                                                 String namespace,
                                                 String accession ){
        
        return dhtRouter.getDhtRouterList( provider, service,
                                           namespace, accession );
    }
    
}
