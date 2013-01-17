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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class McClient implements CacheClient {

    private Log log = LogFactory.getLog( McClient.class );

    // configuration properties
    //-------------------------

    private MemcachedClient mcf = null;    
    private int ttl = 0;

    //*** setter
    public void setMcf( MemcachedClient mcf ) {
        this.mcf = mcf;
    }
    
    public void setTtl( int timeToLive ) {
        ttl = timeToLive;
    }

    // CacheClient implementation
    //---------------------------

    public Object fetch( String id ) {
        try {
            return mcf.get( id );
        } catch ( Exception ex ) { 
            log.warn( "memcache fetch got exception: " + ex.toString() );
            return null;
        }
    }
    
    public void store( String id, Object obj ) {
        try {
            mcf.set( id, ttl, obj );
        } catch ( Exception ex ) {
            log.warn( "memcache store got exception: " + ex.toString() );
            throw null;
        }
    }
    
}

