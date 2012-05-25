package edu.ucla.mbi.monitor;

/* #=========================================================================
 # $Id::                                                                    $
 # Version: $Rev::                                                          $
 #===========================================================================
 #
 # NativeAgent: 
 #    monitors/updates native records
 #   
 #
 #======================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.cache.orm.*;

import edu.ucla.mbi.services.ServiceException;

public class NativeAgent implements Agent {

    WSContext context = null;

    public void setContext( WSContext context ) {

        this.context = context;

        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "configured" );
    }

    public void run() {

        Log log = LogFactory.getLog( NativeAgent.class );
        log.info( "running" );

        NativeRecordDAO ndo = DipProxyDAO.getNativeRecordDAO();

        if ( ndo != null ) {

            // go over providers
            // ------------------

            Set<String> providers = context.getServices().keySet();

            for ( Iterator<String> ii = providers.iterator(); ii.hasNext(); ) {

                String prv = ii.next();

                RemoteServerContext rsc = WSContext.getServerContext( prv );

                try {
                    List<String[]> oldList = ndo.getExpireFirst( prv );

                    for ( Iterator<String[]> jj = oldList.iterator(); jj
                            .hasNext(); ) {

                        String[] old = jj.next();

                        String service = old[0];
                        String ns = old[1];
                        String ac = old[2];

                        log.info( "updating: " + prv + ":" + service + ":" + ns
                                + ":" + ac );

                        try {
                            NativeRecord natRec =
                                    rsc.getNativeServer().getNative( prv,
                                            service, ns, ac, rsc.getTimeout() );

                            NativeRecord oldRecord =
                                    ndo.find( prv, service, ns, ac );

                            log.info( "Native rec: " + oldRecord );

                            if ( oldRecord == null ) {
                                oldRecord =
                                        new NativeRecord( prv, service, ns, ac );
                            }

                            oldRecord.setNativeXml( natRec.getNativeXml() );
                            oldRecord.resetExpireTime( Calendar.getInstance()
                                    .getTime(), rsc.getTtl() );

                            // store native record locally
                            ndo.create( oldRecord );

                        } catch ( ServiceException se ) {
                            log.info( "remote service (" + prv + ") " + se );
                        }
                    }
                } catch ( Exception e ) {
                    log.info( "exception(" + prv + ") " + e );
                }
            }
        }
        log.info( "done" );
    }
}
