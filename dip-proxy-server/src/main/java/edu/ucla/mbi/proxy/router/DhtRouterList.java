package edu.ucla.mbi.proxy.router;

/*===========================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/service#$
 * $Id:: CachingService.java 130 2009-02-03 17:58:49Z wyu                   $
 * Version: $Rev:: 130                                                      $
 *===========================================================================
 *
 * DhtRouterList:
 *  
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ow.id.*;
import ow.dht.*;
import ow.messaging.*;
import ow.routing.*;

import java.net.*;
import java.util.*;

import java.io.Serializable;
import java.math.BigInteger;

import edu.ucla.mbi.proxy.*;

public class DhtRouterList implements Serializable {
    
    private List<DhtRouterItem> items = new ArrayList<DhtRouterItem>();
    
    public DhtRouterItem getItem( int i){ 
	return items.get( i );
    }
    
    public void addItem( DhtRouterItem item ) {
        items.add( item );
    }

    public void removeItem( int index) {
        items.remove( index ) ;
    }

    public void removeItem( DhtRouterItem item ) {
        items.remove( item );
    }

    public int size() {
        return items.size();
    }
    
    public Iterator<DhtRouterItem> iterator(){
        return items.iterator();
    }

}
