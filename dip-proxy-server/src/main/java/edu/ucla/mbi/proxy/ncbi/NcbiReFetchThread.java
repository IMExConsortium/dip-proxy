package edu.ucla.mbi.proxy.ncbi;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * NcbiReFetchThread:
 *    in case NCBI web service returns empty set, the thread will refetch again
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.context.WSContext;
import edu.ucla.mbi.proxy.NativeRestServer;
import edu.ucla.mbi.cache.NativeRecord;
import edu.ucla.mbi.fault.*;
import edu.ucla.mbi.cache.orm.*;
import java.util.*;


import javax.xml.xpath.*;


public class NcbiReFetchThread extends Thread {

    private Log log = LogFactory.getLog( NcbiReFetchThread.class );
    private String ns, ac;
    private String nlmid = "";

    private int ttl;
    private int timeout;
    private int threadRunMinutes;
    private long waitMillis; 

    private String provider = "NCBI";
    private String service = "nlm";
    private WSContext wsContext = null;
    private NcbiGetJournal ncbiGetJournal = null;

    private boolean verify = false;


    public NcbiReFetchThread( String ns, String ac,
                              int timeout, int threadRunMinutes, 
                              NcbiGetJournal ncbiGetJournal,
                              WSContext context ) {
        this.ns = ns;
        this.ac = ac;
        this.timeout = timeout;
        this.threadRunMinutes = threadRunMinutes;
        this.waitMillis = threadRunMinutes * 1000; //millisecond
        this.ncbiGetJournal = ncbiGetJournal;
        this.wsContext = context;
    }

    public NcbiReFetchThread( String ns, String ac, boolean verify,
                              int timeout, int threadRunMinutes, 
                              NcbiGetJournal ncbiGetJournal,
                              WSContext context ) {
        this.ns = ns;
        this.ac = ac;
        this.verify = verify;
        this.timeout = timeout;
        this.threadRunMinutes = threadRunMinutes;
        this.waitMillis = threadRunMinutes * 1000;
        this.ncbiGetJournal = ncbiGetJournal;
        this.wsContext = context;
    }

    public void start_verify(){
        verify = true;
        log.info( "start_verify start. " );
        this.start();
    }

    public void start_no_verify(){
        verify = false;
        log.info( "start_no_verify start. " );
        this.start();
    }

    public void run(){
        log.info( "NcbiFetchThread running... " ); 
        
        wsContext.threadCountUp();

        String retVal = null;
        NativeRecord record = null;
        NativeRecordDAO nativeRecordDAO = 
            wsContext.getDipProxyDAO().getNativeRecordDAO();
        

        long startTime = System.currentTimeMillis();

        if( ! wsContext.isDbCacheOn( "NCBI" ) ) return;     

        if( ns == null || ac == null ) return;

        //----------------------------------------------------------------------
        // esearch ncbi internal id of the nlmid
        //----------------------------------------------------------------------

        if( verify ) {
            log.info( "NcbiReFetchThread: nlmid is empty. " );
            while ( System.currentTimeMillis() - startTime < waitMillis ) {
                
                try{
                    nlmid = ncbiGetJournal.esearch( ac );
                } catch( ServerFault sf ){
                    //
                } 
            
                if( !nlmid.equals("") ){
                    break;
                }
            }
        } else {
            nlmid = ac;
        }
        
        //----------------------------------------------------------------------                
        // efetch real nlmid 
        //----------------------------------------------------------------------

        if( !nlmid.equals( "" ) ) {
            log.info( "after esearch: nlmid is " + nlmid );
            startTime = System.currentTimeMillis();            

            while ( System.currentTimeMillis() - startTime < waitMillis ) {
                try{
                    record = ncbiGetJournal.efetch( ns, nlmid, timeout );
                    log.info( "after efetch: record=" + record );
                } catch( ServerFault sf ){
                    //
                } 
                
                if( record != null ) {
                    break;
                }
            }

            if( record != null ) {

                synchronized( nativeRecordDAO ) {
                    
                    NativeRecord cacheRecord = nativeRecordDAO
                        .find( provider, service, ns, ac );
                    
                    log.info( "cachedRecord=" + cacheRecord );
                    if( cacheRecord != null ) {
                        record.setId( cacheRecord.getId() );
                        record.setCreateTime( cacheRecord.getCreateTime() );
                    }
                    
                    record.resetExpireTime( wsContext.getTtl( "NCBI" ) );
                    
                    nativeRecordDAO.create( record );
                    
                    log.info( "NcbiReFetchThread: getNative: native record " +
                              "is create/updated.");
                }
            }
        }

        wsContext.threadCountDown();

        log.info( "NcbiReFetchThread: final DONE. " );
    }
}
