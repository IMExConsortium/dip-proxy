package edu.ucla.mbi.proxy.router;

/*===========================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/service#$
 * $Id:: CachingService.java 130 2009-02-03 17:58:49Z wyu                   $
 * Version: $Rev:: 130                                                      $
 *===========================================================================
 *
 * DhtRouterItem:
 *  
 *
 *========================================================================= */

import java.io.Serializable;
import java.net.InetAddress;

import java.util.Calendar;

import edu.ucla.mbi.proxy.*;

public class DhtRouterItem implements Serializable {
    
    //private InetAddress ip;
    //private int port;

    private String address;
    
    private long create;
    private long expire;
    
    public DhtRouterItem( String address,
                          long create, long expire ) {

        this.address = address;
        this.create = create;
        this.expire = expire;
    }

    public String getAddress() {
        return address;
    }
    
    public long getCreateTime() {
        return create;
    }

    public long getExpireTime() {
        return expire;
    }

    public void setCreateTime( long time ) {
        create = time;
    }

    public void setExpireTime( long time ) {
        expire = time;
    }

    public String toString(){

        Calendar cre = Calendar.getInstance();
        cre.setTimeInMillis(create);
        
        Calendar exp = Calendar.getInstance();
        exp.setTimeInMillis(expire);
        
        return this.address + 
            " (" + cre.getTimeInMillis() + "|" + 
            exp.getTimeInMillis() + ")"; 
    }
    
}
