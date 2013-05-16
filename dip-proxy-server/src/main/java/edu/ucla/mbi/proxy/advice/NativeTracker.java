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
import edu.ucla.mbi.proxy.context.WSContext;
import edu.ucla.mbi.proxy.ProxyFault;
import edu.ucla.mbi.fault.*;

public class NativeTracker {

    public static final int DEFAULT_INTERVAL = 0;
    private int interval = DEFAULT_INTERVAL;  // units: milliseconds

    private WSContext wsContext;

    public void setWsContext(WSContext context){
        this.wsContext = context;
    }
    
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

    public Object invoke( ProceedingJoinPoint pjp ) throws Throwable {

        java.lang.Object[] args = pjp.getArgs();
        
        Log log = LogFactory.getLog( NativeTracker.class );
        log.info( "called:" +  
                  " prv=" + args[0] + " srv=" + args[1] + 
                  " ns=" + args[2] + " ac=" + args[3] );
        
        NativeAuditDAO nad = 
            wsContext.getDipProxyDAO().getNativeAuditDAO();

        NativeAudit lastAudit = nad.findLast( (String) args[0],
                                              (String) args[1] );
        
        Calendar beforeCal = Calendar.getInstance();

        Object returnValue = null;
        int status = 0;

        try {
            returnValue = pjp.proceed();
        } catch ( ProxyFault fault ) {
            log.info( "invoke: proxy fault code: " 
                      + fault.getFaultInfo().getFaultCode() );
            log.info( "invoke: proxy fault message: " 
                      + fault.getFaultInfo().getMessage() );

            status = fault.getFaultInfo().getFaultCode();
        }

        Calendar afterCal = Calendar.getInstance();

        long resTime = afterCal.getTimeInMillis() - 
                            beforeCal.getTimeInMillis() ;
        
        log.info( " response time: " + resTime );
        
        NativeAudit nextAudit = null;

        if (lastAudit != null ) {

            log.info( "last audit:" + 
                      " prv=" + lastAudit.getProvider() + 
                      " srv=" + lastAudit.getService() +
                      " ns=" + lastAudit.getNs() + 
                      " ac=" + lastAudit.getAc() +
                      " time=" + lastAudit.getTime() );

            if( ( afterCal.getTimeInMillis() - 
                    lastAudit.getTime().getTime() > interval )
                        && status == 0  ) 
            {
                nextAudit = new NativeAudit( (String) args[0],
                                             (String) args[1],
                                             (String) args[2],
                                             (String) args[3],
                                             beforeCal.getTime(),
                                             resTime );
            } 
        } else {
            if( status == 0 ) {
                nextAudit = new NativeAudit( (String) args[0],
                                             (String) args[1],
                                             (String) args[2],
                                             (String) args[3],
                                             beforeCal.getTime(),
                                             resTime );
            } 
        }

        if( status != 0 ) {
            nextAudit = new NativeAudit( (String) args[0],
                                         (String) args[1],
                                         (String) args[2],
                                         (String) args[3],
                                         beforeCal.getTime(),
                                         resTime, status );
        }

        if ( nextAudit != null ) {
            log.info( " audit archived" );
            nad.create( nextAudit );
        }
        
        log.info( "DONE" );

        if( status != 0 ) {
            throw FaultFactory.newInstance( status );
        } else {
            return returnValue;
        }
    }    
}
