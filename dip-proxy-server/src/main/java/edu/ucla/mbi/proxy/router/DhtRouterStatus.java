package edu.ucla.mbi.proxy.router;

/*==============================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/service#   $
 * $Id:: CachingService.java 130 2009-02-03 17:58:49Z wyu                      $
 * Version: $Rev:: 130                                                         $
 *==============================================================================
 *
 * DhtRouter:
 *   selects remote server to call
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
        
        ID id = dhtRouter.getRecordID( provider, service,
                                       namespace, accession );
        return dhtRouter.getDhtRouterList( id );
    }
    
    public List<DhtRouterList> getDhtRouterList( ID rid ){
        return dhtRouter.getDhtRouterList( rid );
    }
}
