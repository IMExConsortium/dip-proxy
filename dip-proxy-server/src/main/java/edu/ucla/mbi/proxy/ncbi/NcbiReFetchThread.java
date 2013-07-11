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
    
    public NcbiReFetchThread( String ns, String ac, String nlmid,
                              int timeout, int threadRunMinutes, 
                              NcbiGetJournal ncbiGetJournal,
                              WSContext context ) {
        this.ns = ns;
        this.ac = ac;
        this.nlmid = nlmid;
        this.timeout = timeout;
        this.threadRunMinutes = threadRunMinutes;
        this.waitMillis = threadRunMinutes * 60 * 1000;
        this.ncbiGetJournal = ncbiGetJournal;
        this.wsContext = context;
    }

    public void run() {
        log.info( "NcbiFetchThread running... " ); 
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
        if( nlmid.equals( "" ) ) {
            log.info( "NcbiReFetchThread: nlmid is empty. " );
            while ( System.currentTimeMillis() - startTime < waitMillis ) {

                nlmid = ncbiGetJournal.esearch( ns, ac, timeout,
                                                threadRunMinutes, false );

                if( !nlmid.equals("") ){
                    break;
                }
            }
        }
                
        //----------------------------------------------------------------------                
        // efetch real nlmid 
        //----------------------------------------------------------------------

        if( !nlmid.equals( "" ) ) {
            log.info( "after esearch: nlmid is " + nlmid );
            startTime = System.currentTimeMillis();            

            while ( System.currentTimeMillis() - startTime < waitMillis ) {
                record = ncbiGetJournal.efetch( ns, nlmid, timeout,
                                                threadRunMinutes, false );
                log.info( "after efetch: record=" + record );
                if( record != null ) {
                    break;
                }
            }

            if( record != null ) {
    
                synchronized( nativeRecordDAO ) {
                    try {
                       
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

                    } catch ( Exception ex ) {
                        throw new RuntimeException( "TRANSACTION" ) ;
                    }
                }

            }
        }

        log.info( "NcbiReFetchThread: final DONE. " );
    }
}
