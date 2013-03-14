package edu.ucla.mbi.cache.orm;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DxfRecordDAO extends AbstractDAO {

    private Log log = LogFactory.getLog( DxfRecordDAO.class );
  
    public DxfRecordDAO ( HibernateOrmUtil util ) {
        super( util );
    }
 
    public void create( DxfRecord dxfr ) throws DAOException {
        this.saveOrUpdate( dxfr );
    }

    public void saveOrUpdate( DxfRecord dxfr )
        throws DAOException {

        try {
            super.saveOrUpdate ( dxfr );
        
        } catch( DAOException ex ){
            
            log.warn ( "saveOrUpdate get excepiton. then call find(). " );

            DxfRecord dxfRecord = this.find( dxfr.getProvider(), 
                dxfr.getService(), dxfr.getNs(), dxfr.getAc(), 
                dxfr.getDetail() );

            if( dxfRecord != null ) {
                dxfr.setId( dxfRecord.getId() );
                super.update ( dxfr );
            } else {
                throw ex;
            }
        }
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

    public Long countAll( String provider, String service
                          ) throws DAOException {

        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        Long count = null;
        try {
            Query query = session
                .createQuery( "SELECT count (*) " +
                              " FROM DxfRecord dr " +
                              " WHERE dr.provider = :prv AND " +
                              " dr.service = :srv " );

            query.setParameter( "prv", provider.toUpperCase() );
            query.setParameter( "srv", service.toLowerCase() );
            count = (Long) query.uniqueResult();
            tx.commit();

        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }

        return count;
    }

    //--------------------------------------------------------------------------
    //--------------------------------------------------------------------------
    public Long countAll( String provider
                          ) throws DAOException {

        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        
        Long count = null;

        log.info( "coutAll: before try. ");
        try {

            Query query = session
                .createQuery( "SELECT count (*) " +
                              " FROM DxfRecord dr " +
                              " WHERE dr.provider = :prv " );

            query.setParameter( "prv", provider.toUpperCase() );

            count = (Long) query.uniqueResult();

            tx.commit();

        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
        log.info( "countAll: after try. ");
        return count;
    }

    //--------------------------------------------------------------------------
    public void removeAll( String provider, String service 
                           ) throws DAOException {

        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            Query query = session.createQuery( "DELETE FROM DxfRecord dr "  +
                                               " WHERE dr.provider = :prv " +
                                               " AND dr.service = :srv " );

            query.setParameter( "prv", provider.toUpperCase() );
            query.setParameter( "srv", service.toLowerCase() );
            query.executeUpdate();
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
    }

    //--------------------------------------------------------------------------
    public void removeAll( String provider ) throws DAOException {
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            Query query = session.createQuery( "DELETE FROM DxfRecord dr "  +
                                               " WHERE dr.provider = :prv ");

            query.setParameter( "prv", provider.toUpperCase() );
            query.executeUpdate();
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
    }

    //--------------------------------------------------------------------------

    public void expireAll( String provider, String service ) throws DAOException {
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        Date now = Calendar.getInstance().getTime();

        log.info( "before try. " );
        try {
            Query query = session.createQuery(
                "UPDATE DxfRecord dr set dr.expireTime = :now " +
                " WHERE dr.provider = :prv " +
                " AND dr.service = :srv " );

            query.setParameter( "now", now );
            query.setParameter( "prv", provider.toUpperCase() );
            query.setParameter( "srv", service.toLowerCase() );

            query.executeUpdate();
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
        log.info( "after try. " );
    }
    //--------------------------------------------------------------------------

    public void expireAll( String provider ) throws DAOException {
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        Date now = Calendar.getInstance().getTime();

        log.info( "before try. " );
        try {
            Query query = session.createQuery(
                "UPDATE DxfRecord dr set dr.expireTime = :now " +
                " WHERE dr.provider = :prv ");

            query.setParameter( "now", now );
            query.setParameter( "prv", provider.toUpperCase() );
            query.executeUpdate();
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
        log.info( "after try. " );
    }

}
