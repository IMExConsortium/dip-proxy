package edu.ucla.mbi.cache.orm;

/*===========================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/cache/o#$
 * $Id:: DxfRecordDAO.java 100 2009-01-01 22:23:35Z lukasz                  $
 * Version: $Rev:: 100                                                      $
 *===========================================================================
 *
 * DxfRecordDAO:
 *
 *========================================================================= */

import org.hibernate.*;
  
import edu.ucla.mbi.orm.*;
import edu.ucla.mbi.cache.*;

import java.net.*;
import java.util.*;

public class DxfRecordDAO extends AbstractDAO {
  
    public DxfRecordDAO ( HibernateOrmUtil util ) {
        super( util );
    }
 
    public void create( DxfRecord dxfr ) throws DAOException {
        super.saveOrUpdate( dxfr );
    }

    public void delete ( DxfRecord dxfr ) throws DAOException{
       super.delete( dxfr );
    }

    public DxfRecord find(int id) throws DAOException{
        DxfRecord dxfr = (DxfRecord) super.find(DxfRecord.class, id);
        return dxfr;
    }
    
    public DxfRecord find( String provider, String service, 
                           String ns, String ac, String detail 
                           ) throws DAOException {
        
        DxfRecord dxfr = null; 
        
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        
        try {

            Query query = session
                .createQuery( "from DxfRecord dr where " + 
                              " dr.provider = :prv and " +
                              " dr.service = :srv and " +
                              " dr.ns = :ns and " + 
                              " dr.ac = :ac and " +
                              " dr.detail = :dtl");
            
            query.setParameter( "prv", provider.toUpperCase() );
            query.setParameter( "srv", service.toLowerCase() );
            query.setParameter( "ns", ns );
            query.setParameter( "ac", ac );
            query.setParameter( "dtl", detail );
            query.setFirstResult( 0 );
            dxfr = (DxfRecord) query.uniqueResult();
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
 
	    
        return dxfr;
    }
}
