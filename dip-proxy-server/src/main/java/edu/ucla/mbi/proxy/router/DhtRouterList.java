package edu.ucla.mbi.proxy.router;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
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
    
    private ID id;
    
    public DhtRouterList( ID id ){ 
        this.id = id;
    }

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

    //--------------------------------------------------------------------------
    
    boolean equals( Object obj ){
        
        if ( this == obj ) return true;
        if ( obj == null || !(obj instanceof DhtRouterList ) ) return false;
        
        DhtRouterList drl = (DhtRouterList) obj;
        return this.id.equals( drl.getId() );
    }

    public int hashCode() {
        return this.id.hashCode();
    }
    
}
