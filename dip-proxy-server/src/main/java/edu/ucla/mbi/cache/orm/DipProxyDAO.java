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
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.proxy.ProxyFault;
import edu.ucla.mbi.orm.*;
import edu.ucla.mbi.fault.*;

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

    public static void createNativeRecord ( NativeRecord record ) 
        throws ProxyFault {
        
        try {
            nativeRecordDAO.create( record );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public static void deleteNativeRecord ( NativeRecord record )
        throws ProxyFault {
        
        try {
            nativeRecordDAO.delete( record );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public static NativeRecord findNativeRecord ( String provider, 
        String service, String ns, String ac ) throws ProxyFault {
        
        try {
            return nativeRecordDAO.find( provider, service, ns, ac );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public static void createDxfRecord ( DxfRecord record )
        throws ProxyFault {

        try {
            dxfRecordDAO.create( record );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public static void deleteDxfRecord ( DxfRecord record )
        throws ProxyFault {

        try {
            dxfRecordDAO.delete( record );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public static DxfRecord findDxfRecord ( String provider, String service,
        String ns, String ac, String detail ) throws ProxyFault {

        try {
            return dxfRecordDAO.find( provider, service, ns, ac, detail );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    
}
