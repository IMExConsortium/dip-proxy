package edu.ucla.mbi.util.cache;

/* =============================================================================
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *                                                                             $
 * McClient: memcached-based CacheClient implementation                        $
 *                                                                             $
 *     TO DO:  implement :o)                                                   $
 *                                                                             $
 *=========================================================================== */

import edu.ucla.mbi.util.cache.CacheClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;
import net.spy.memcached.MemcachedClient;
public class McClient implements CacheClient {

    // configuration properties
    //-------------------------

    private MemcachedClient mcFactory = null;
    
    public void setMcf( MemcachedClient mcFactory ){
        
        this.mcFactory = mcFactory;
    }
    
    private long ttl = 0;

    public void setTtl( long timeToLive ){
        
        ttl = timeToLive;

    }

    // CacheClient implementation
    //---------------------------

    public Object fetch( String id ){
        return null;
    }
    
    public void store( String id, Object obj ){

    }
    
}

