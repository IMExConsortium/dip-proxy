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
 
    private NativeRecordDAO nativeRecordDAO; 
    private DxfRecordDAO dxfRecordDAO; 
    private NativeAuditDAO nativeAuditDAO;
   
    //*** setters
    public void setNativeRecordDAO ( NativeRecordDAO dao) {
        this.nativeRecordDAO = dao;
    }
    
    public void setDxfRecordDAO( DxfRecordDAO dao) {
        this.dxfRecordDAO = dao;
    }

    public void setNativeAuditDAO ( NativeAuditDAO dao) {
        this.nativeAuditDAO = dao;
    }
    
    //*** getter

    public NativeRecordDAO getNativeRecordDAO () {
        return nativeRecordDAO;
    }

    public DxfRecordDAO getDxfRecordDAO () {
        return dxfRecordDAO;
    }

    public NativeAuditDAO getNativeAuditDAO () {
        return nativeAuditDAO;
    }

    public void createNativeRecord ( NativeRecord record ) 
        throws ProxyFault {
        
        try {
            nativeRecordDAO.create( record );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public void deleteNativeRecord ( NativeRecord record )
        throws ProxyFault {
        
        try {
            nativeRecordDAO.delete( record );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public NativeRecord findNativeRecord ( String provider, 
        String service, String ns, String ac ) throws ProxyFault {
        
        try {
            return nativeRecordDAO.find( provider, service, ns, ac );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public void createDxfRecord ( DxfRecord record )
        throws ProxyFault {

        try {
            dxfRecordDAO.create( record );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public void deleteDxfRecord ( DxfRecord record )
        throws ProxyFault {

        try {
            dxfRecordDAO.delete( record );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    public DxfRecord findDxfRecord ( String provider, String service,
        String ns, String ac, String detail ) throws ProxyFault {

        try {
            return dxfRecordDAO.find( provider, service, ns, ac, detail );
        } catch ( DAOException ex ) {
            throw FaultFactory.newInstance( Fault.TRANSACTION );
        }
    }

    
}
