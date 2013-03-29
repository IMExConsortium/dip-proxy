package edu.ucla.mbi.monitor;

/* #============================================================================
   # $Id::                                                                     $
   # Version: $Rev::                                                           $
   #============================================================================
   #
   # Cache monitor: 
   #   
   #   
   #
   #========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.ucla.mbi.server.WSContext;

public class Scheduler {

    ScheduledThreadPoolExecutor executor = null;

    //WSContext context = null;
    
    private int threadCount = 0;
    private Map agents = null;

    public static final int POOL_SIZE = 3;

    public Scheduler(){}

    //---------------------------------------------------------------------
    
    /*
    public void setContext( WSContext context ) {
        this.context = context;
    } */

    //---------------------------------------------------------------------


    public void setThreadCount( String count ){
        
        Log log = LogFactory.getLog( WSContext.class );

        int icount = 0;
        if ( count != null ) {
            if (count.replaceAll( "\\s+", "" ).matches( "^\\d+$" ) ) {
                try {
                    // detault units: seconds
                    icount = Integer.parseInt( count );
                    log.info( "set: thread count=" + icount );
                } catch ( NumberFormatException nfe ) {
                    log.info( "thread count=" + count +
                              " :format error." );
                }
            } else {
                log.info("thread count=" + count +
                         " :unknown units/format.");
            }
        }
        threadCount = icount;
    }

    //---------------------------------------------------------------------


    public void setAgents( Map agents ) {
        this.agents = agents;
    }

    //---------------------------------------------------------------------


    public void start() {
        
        Log log = LogFactory.getLog( Scheduler.class );
        log.info( "starting" );
        
        if ( threadCount > 0 && agents !=null ) {
            executor = new ScheduledThreadPoolExecutor( threadCount );

            // schedule agents
            //----------------
            
            for ( Iterator ii = agents.keySet().iterator(); ii.hasNext(); ) {
                
                Map agent = (Map) agents.get( ii.next() );
                
                String interval = (String) agent.get( "interval" );
                Agent agentInstance = (Agent) agent.get( "agent-instance" );
                
                //agentInstance.setContext( context );

                executor.scheduleAtFixedRate( agentInstance, 0L, 
                                              1000L*parseInterval( interval ), 
                                              TimeUnit.MILLISECONDS );
            }
        }
    }
    
    public void shutdown() {
        
        Log log = LogFactory.getLog( Scheduler.class );
        
        if ( executor != null ){
            log.info( "shutdown called" );
            executor.shutdown();
            log.info( "shutdown complete" );
        }
    }

    //---------------------------------------------------------------------


    private int parseInterval( String interval ){
        
        Log log = LogFactory.getLog( Scheduler.class );

        int irate = 0;
        if ( interval != null ) {
            if (interval.replaceAll( "\\s+", "" ).matches( "^\\d+$" ) ) {
                try {
                    // detault units: seconds
                    irate = Integer.parseInt( interval );
                    log.info( "  interval=" + irate + "s");
                } catch ( NumberFormatException nfe ) {
                    log.info( "  interval=" + interval +
                              " :format error." );
                }
            } else {
                log.info("  interval=" + interval +
                         " :unknown units/format.");
            }
        }
        return irate;
    }
    
}
