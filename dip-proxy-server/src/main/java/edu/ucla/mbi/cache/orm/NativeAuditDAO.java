package edu.ucla.mbi.cache.orm;

/*===========================================================================
 * $HeadURL::                                                               $
 * $Id::                                                                    $
 * Version: $Rev::                                                          $
 *===========================================================================
 *
 * NativeAuditDAO:
 *
 *========================================================================= */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.*;
import java.util.*;

import org.hibernate.*;
import edu.ucla.mbi.orm.*;
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.server.*;

public class NativeAuditDAO extends AbstractDAO {

    public NativeAuditDAO ( HibernateOrmUtil util ) {
        super( util );
    }

    public void create( NativeAudit nativer 
                        ) throws DAOException {
        saveOrUpdate( nativer );
    }

    public void delete( NativeAudit nativer 
                        ) throws DAOException {
        super.delete( nativer );
    }

    public NativeAudit find( String provider, String service,
                             String ns, String ac 
                             ) throws DAOException {
        
        NativeAudit nativer = null;
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            Query query = session
                .createQuery( "from NativeAudit nr where "+
                              " nr.provider = :prv and " +
                              " nr.service = :srv and " +
                              " nr.ns = :ns and nr.ac = :ac " );
            
            query.setParameter("prv", provider );
            query.setParameter("srv", service );
            query.setParameter("ns", ns );
            query.setParameter("ac", ac );
            query.setFirstResult( 0 );
            nativer = (NativeAudit) query.uniqueResult();
            tx.commit();	
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
        
	    return nativer;
    }
    //--------------------------------------------------------------------------
    
    public NativeAudit findLast( String provider, String service ) 
        throws DAOException {
        
        // get oldest entries
        //-------------------

        NativeAudit lastAudit = new NativeAudit();

        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
        
            Query query = session
                .createQuery( "select na " +
                              " from NativeAudit na " +
                              " where na.provider = :prv " +
                              "   and na.service = :srv  " +
                              " order by na.time desc " );
            query.setParameter( "prv", provider );
            query.setParameter( "srv", service );
            query.setMaxResults(1);
            lastAudit = (NativeAudit) query.uniqueResult();
            
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
 
        return lastAudit;
    }


    public List<long[]> findLastList( String provider, String service,
                                     int count ) 
        throws DAOException {

        List<long[]> result = new ArrayList();
        
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {

            Query query = session
                .createQuery( "select na.time, na.responseTime, na.status " +
                              " from NativeAudit na " +
                              " where na.provider = :prv " +
                              " and na.service = :srv  " +
                              " order by na.time desc " );
            
            query.setParameter( "prv", provider );
            query.setParameter( "srv", service );
            query.setMaxResults( count );
            
            List<Object[]> rlist = query.list(); 
            
            for ( Iterator<Object[]> ii = rlist.iterator(); 
                  ii.hasNext(); ) {
                
                Object[] i = ii.next();
                long[] pair = new long[3];
                pair[0] = ((Date) i[0]).getTime(); // time in seconds
                pair[1] = ((Long) i[1]).longValue();
                pair[2] = ((Integer) i[2] ).intValue();
                result.add( pair );
            }
            
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }

        return result;
    }

    //--------------------------------------------------------------------------


    public Map<String,Double> delayAll( String provider
                                        ) throws DAOException {

        Log log = LogFactory.getLog( NativeAuditDAO.class );
        
        Map<String,Double> result = new HashMap<String,Double>();
        
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            
            // get services
            //-------------
            log.info("delayAll: before wsContext.");
            Set<String> services = WSContext.getServerContext( provider )
                                            .getTransformer().getTransfMap()
                                            .keySet();
            log.info("delayAll: afer wsContext.");
            // get newest entries
            //-------------------
                
            for ( Iterator<String> ii = services.iterator();
                  ii.hasNext(); ) {

                String service = ii.next();
                
                Query query = session
                    .createQuery( "select na.responseTime  " +
                                  " from NativeAudit na " +
                                  " where na.provider = :prv " +
                                  " and na.service = :srv" +
                                  " and na.id = " +
                                  " (select max(na.id) " +
                                  "   from NativeAudit na " +
                                  "   where na.provider = :prv " +
                                  "   and na.service = :srv ) " );

                query.setParameter( "prv", provider );
                query.setParameter( "srv", service );
                query.setMaxResults(1);
                Long delay = (Long) query.uniqueResult();

                if( delay != null ) {                
                    log.info( "prv=" + provider + " srv=" + service + " del=" + delay);
                    result.put( service, delay/1000.0 );            
                }
            }
            log.info("delayAll: after loop.");
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        } 
        
        return result;
    }
}
