package edu.ucla.mbi.cache.orm;

/*===========================================================================
 * $HeadURL:: http://imex.mbi.ucla.edu/svn/ProxyWS/src/edu/ucla/mbi/cache/o#$
 * $Id:: NativeRecordDAO.java 100 2009-01-01 22:23:35Z lukasz               $
 * Version: $Rev:: 100                                                      $
 *===========================================================================
 *
 * NativeRecordDAO:
 *
 *========================================================================= */

import org.hibernate.*;
import edu.ucla.mbi.cache.*;
import edu.ucla.mbi.orm.*;

import java.net.*;
import java.util.*;

public class NativeRecordDAO extends AbstractDAO {

    public NativeRecordDAO ( HibernateOrmUtil util ) {
        super( util );
    } 

    public void create( NativeRecord nativer 
                        ) throws DAOException {
        saveOrUpdate( nativer );
    }

    public void delete( NativeRecord nativer 
                        ) throws DAOException {
        super.delete( nativer );
    }

    public NativeRecord find(int id) throws DAOException{
        NativeRecord nativer = (NativeRecord) super.find (NativeRecord.class, id);
        return nativer;
    }

    public NativeRecord find( String provider, String service,
                              String ns, String ac 
                              ) throws DAOException {
        
        NativeRecord nativer = null;

        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        try {
            Query query = session
                .createQuery( "from NativeRecord nr where "+
                              " nr.provider = :prv and " +
                              " nr.service = :srv and " +
                              " nr.ns = :ns and nr.ac = :ac " );
            
            query.setParameter("prv", provider.toUpperCase() );
            query.setParameter("srv", service.toLowerCase() );
            query.setParameter("ns", ns );
            query.setParameter("ac", ac );
            query.setFirstResult( 0 );
            nativer = (NativeRecord) query.uniqueResult();
            tx.commit();	
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        } 
        
	    return nativer;
    }

    public long count( String provider, String service 
                       ) throws DAOException {
        
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        
        Long count = null;
        
        try {
            Query query = session
                .createQuery( "select count (*) "+
                              " from NativeRecord nr "+
                              " where nr.provider = :prv and " +
                              "  nr.service = :srv" );
            
            query.setParameter( "prv", provider.toUpperCase() );
            query.setParameter( "srv", service.toLowerCase() );

            count = (Long) query.uniqueResult();
            tx.commit();
            
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        } 
        
        if ( count != null ){
            return count.longValue();
        } else {
            return 0;
        }
    }

    //---------------------------------------------------------------------

    public long count( String provider 
                      ) throws DAOException {
        
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        Long count = null;
        try {
            Query query = session
                .createQuery( "select count (*) " + 
                              " from NativeRecord nr " +
                              " where nr.provider = :prv " );
            
            query.setParameter( "prv", provider.toUpperCase() );
        
            count = (Long) query.uniqueResult();
            tx.commit();
            
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        } 

        if ( count != null ){
            return count.longValue();
        } else {
            return 0;
        }
    }

    public Map<String,Long> countAll( String provider 
                                      ) throws DAOException {
        
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        
        Map result = new HashMap<String,Integer>();
        
        try {
            Query query = session
                .createQuery( "select nr.service, count (*) " + 
                              " from NativeRecord nr " +
                              " where nr.provider = :prv " +
                              "  group by nr.service" );
            
            query.setParameter( "prv", provider.toUpperCase() );
        
            List counts =  query.list();
            
            for ( Iterator ii = counts.iterator(); ii.hasNext(); ) {
                
                Object[] row = (Object[]) ii.next();
                
                String service = (String) row[0];
                Long count = (Long) row[1];

                result.put( service, count );
            }            
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
 
        return result;
    }


    //---------------------------------------------------------------------
    
    public List<String[]> 
        getExpireLast( String provider ) throws DAOException {
        
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        List<String[]> result = new ArrayList<String[]>();

        try {
            
            // get services
            //-------------

            Query serviceQuery = 
                session.createQuery( "select distinct nr.service " +
                                     " from NativeRecord nr " +
                                     " where nr.provider = :prv " );
            
            serviceQuery.setParameter( "prv", provider.toUpperCase() );
            List<String> services = (List<String>) serviceQuery.list();
        
            // get oldest entries
            //-------------------
            
            for ( Iterator<String> ii = services.iterator(); 
                  ii.hasNext(); ) {
                
                String service = ii.next();
                Query query = session
                    .createQuery( "select nr.ns, nr.ac, nr.expireTime " +
                                  " from NativeRecord nr " +
                                  " where nr.expireTime = " +
                                  " (select max(nr.expireTime) " +
                                  "   from NativeRecord nr " +
                                  "   where nr.provider = :prv " +
                                  "   and nr.service = :srv ) " +
                                  " and nr.provider = :prv " +
                                  " and nr.service = :srv" );
                query.setParameter( "prv", provider.toUpperCase() );
                query.setParameter( "srv", service.toLowerCase() );
                List items = query.list();
            
                for ( Iterator<Object[]> jj = items.iterator(); 
                      jj.hasNext(); ) {
                    
                    Object[] j = jj.next();
                    String[] entry = new String[3];
                    entry[0] = service;
                    entry[1] = (String) j[0];
                    entry[2] = (String) j[1];
                    result.add( entry );
                }
            }
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }
        
        return result;
    }

    public List<String[]> 
        getExpireFirst( String provider ) throws DAOException {
        
        Session session = hibernateOrmUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        List<String[]> result = new ArrayList<String[]>();
        
        try {

            // get services
            //-------------

            Query serviceQuery = session
                .createQuery( "select distinct nr.service " +
                              " from NativeRecord nr " +
                              " where nr.provider = :prv " );
            
            serviceQuery.setParameter( "prv", provider.toUpperCase() );
            List<String> services = (List<String>) serviceQuery.list();
        
            // get oldest entries
            //-------------------
            
            for ( Iterator<String> ii = services.iterator(); 
                  ii.hasNext(); ) {
                
                String service = ii.next();
                Query query = session
                    .createQuery( "select nr.ns, nr.ac, nr.expireTime " +
                                  " from NativeRecord nr " +
                                  " where nr.expireTime = " +
                                  " (select min(nr.expireTime) " +
                                  "   from NativeRecord nr " +
                                  "   where nr.provider = :prv " +
                                  "   and nr.service = :srv ) " +
                                  " and nr.provider = :prv " +
                                  " and nr.service = :srv" );
                query.setParameter( "prv", provider.toUpperCase() );
                query.setParameter( "srv", service.toLowerCase() );
                List items = query.list();
                
                for ( Iterator<Object[]> jj = items.iterator(); 
                      jj.hasNext(); ) {
                
                    Object[] j = jj.next();
                    String[] entry = new String[3];
                    entry[0] = service;
                    entry[1] = (String) j[0];
                    entry[2] = (String) j[1];
                    result.add( entry );
                }
            }
            tx.commit();
        } catch ( HibernateException e ) {
            handleException( e );
        } finally {
            session.close();
        }

        return result;
    }
}
