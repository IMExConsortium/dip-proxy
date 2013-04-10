package edu.ucla.mbi.cache.orm;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * DipProxyDAO:
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.orm.HibernateOrmUtil;
import edu.ucla.mbi.server.WSContext;

public class DipProxyDAO {
    private HibernateOrmUtil hibernateOrmUtil;
    private WSContext wsContext;

    public void setWsContext( WSContext context ) {
        this.wsContext = context;
    }

    private static NativeRecordDAO nativeRecordDAO; 
    private static DxfRecordDAO dxfRecordDAO; 
    private static NativeAuditDAO nativeAuditDAO;

    //*** constructor
    public DipProxyDAO( HibernateOrmUtil hibernateOrmUtil ) {
        Log log = LogFactory.getLog( DipProxyDAO.class );
        log.info( "DipProxyDAO aware constructor create." );
        this.hibernateOrmUtil = hibernateOrmUtil;

        this.nativeRecordDAO = new NativeRecordDAO( hibernateOrmUtil, wsContext );
        this.dxfRecordDAO = new DxfRecordDAO ( hibernateOrmUtil );
        this.nativeAuditDAO = new NativeAuditDAO ( hibernateOrmUtil, wsContext );
    }

    //*** getter
    public static NativeRecordDAO getNativeRecordDAO () {
        return nativeRecordDAO;
    }

    public static DxfRecordDAO getDxfRecordDAO () {
        return dxfRecordDAO;
    }

    public static NativeAuditDAO getNativeAuditDAO () {
        return nativeAuditDAO;
    }
}
