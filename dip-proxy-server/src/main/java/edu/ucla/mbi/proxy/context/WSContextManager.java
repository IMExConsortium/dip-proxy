package edu.ucla.mbi.proxy.context;

/*==========================================================================
 * $HeadURL::                                                              $
 * $Id::                                                                   $
 * Version: $Rev::                                                         $
 *==========================================================================
 *
 * WSContextManager: 
 *
 *========================================================================== */

import javax.servlet.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WSContextManager 
    implements javax.servlet.ServletContextListener {

    public void contextInitialized(javax.servlet.ServletContextEvent sce) {
	    System.out.println("WSContextManager: contextInitialized");
    }

    public void contextDestroyed(ServletContextEvent sce) {
	    ClassLoader contextClassLoader = 
	    Thread.currentThread().getContextClassLoader();
	    LogFactory.release(contextClassLoader);
	    System.out.println("WSContextManager: contextDestroyed");
    }
}
