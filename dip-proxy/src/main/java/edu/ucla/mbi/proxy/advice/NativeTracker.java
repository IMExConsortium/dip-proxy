package edu.ucla.mbi.proxy.advice;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * NativeTracker: monitor access to native servers
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import org.aspectj.lang.ProceedingJoinPoint;

import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.cache.orm.*;
import edu.ucla.mbi.proxy.WSContext;

public class NativeTracker {

    public static final int DEFAULT_INTERVAL = 0;
    private int interval = DEFAULT_INTERVAL;  // units: milliseconds
    
    public void initialize(){
        
        Log log = LogFactory.getLog( NativeTracker.class );
        log.info( "NativeAdvice initialized" );
    }

    public void setMinInterval( String interval ){
        
        Log log = LogFactory.getLog( NativeTracker.class );
        if ( interval != null ) {
            if (interval.replaceAll( "\\s+", "" ).matches( "^\\d+$" ) ) {
                try {
                    // detault units: seconds
                    this.interval = 1000*Integer.parseInt( interval );
                    
                } catch ( NumberFormatException nfe ) {
                    log.info( "ProxyWS: ttl=" + interval +
                              " :format error. Using default." );
                }
            } else {
                log.info("ProxyWS: ttl="+interval+
                         " :unknown units/format. Using default.");
            }
        } else {
            log.info( "ProxyWS: ttl not specified: Using default." );
        }
    }

    public Object invoke( ProceedingJoinPoint pjp ) 
        throws Throwable {

        java.lang.Object[] args = pjp.getArgs();
        
        Log log = LogFactory.getLog( NativeTracker.class );
        log.info( "called:" +  
                  " prv=" + args[0] + " srv=" + args[1] + 
                  " ns=" + args[2] + " ac=" + args[3] );
        
        NativeAuditDAO nad = DipProxyDAO.getNativeAuditDAO();

        NativeAudit lastAudit = nad.findLast( (String) args[0],
                                              (String) args[1] );
        
        Calendar beforeCal = Calendar.getInstance();
        Object returnValue = pjp.proceed();
        Calendar afterCal = Calendar.getInstance();

        long resTime = afterCal.getTimeInMillis() - 
            beforeCal.getTimeInMillis() ;
        
        log.info( " response time: " + resTime );
        
        NativeAudit nextAudit = null;

        if (lastAudit != null ){


            log.info( "last audit:" + 
                      " prv=" + lastAudit.getProvider() + 
                      " srv=" + lastAudit.getService() +
                      " ns=" + lastAudit.getNs() + 
                      " ac=" + lastAudit.getAc() +
                      " time=" + lastAudit.getTime() );
            if( afterCal.getTimeInMillis() - 
                lastAudit.getTime().getTime() > interval ) {
            
                nextAudit = new NativeAudit( (String) args[0],
                                             (String) args[1],
                                             (String) args[2],
                                             (String) args[3],
                                             beforeCal.getTime(),
                                             resTime );
            }
            
        } else {
            nextAudit = new NativeAudit( (String) args[0],
                                         (String) args[1],
                                         (String) args[2],
                                         (String) args[3],
                                         beforeCal.getTime(),
                                         resTime );
        }

        if ( nextAudit != null ) {
            log.info( " audit archived" );
            nad.create( nextAudit );
        }
        
        log.info( "DONE" );

        return returnValue;
    }    
}
