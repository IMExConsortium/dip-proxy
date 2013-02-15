package edu.ucla.mbi.proxy.router;

/*==============================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/service#   $
 * $Id:: CachingService.java 130 2009-02-03 17:58:49Z wyu                      $
 * Version: $Rev:: 130                                                         $
 *==============================================================================
 *
 * DhtRouterMessage;
 *  
 *
 *=========================================================================== */

import java.io.Serializable;
import java.net.InetAddress;

import java.util.Calendar;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.NativeRecord;

public class DhtRouterMessage implements Serializable {

    public static final int UPDATE = 0;
    public static final int DELETE = 1;

    private int msg = -1;
    NativeRecord record = null;
    NativeServer server = null;

    public DhtRouterMessage( int msg, NativeRecord record, 
                             NativeServer server ) {
        
        this.msg = msg;
        this.record = record;
        this.server = server;
    }
    
    public int getMsg() {
        return msg;
    }

    public NativeRecord getRecord() {
        return record;
    }

    public NativeServer getNativeServer() {
        return server;
    }
    
}
