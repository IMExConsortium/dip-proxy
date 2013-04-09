package edu.ucla.mbi.proxy;

/* =============================================================================
 # $Id:: PsqStoreDispatchFilter.java 1821 2013-03-16 16:56:02Z lukasz99        $
 # Version: $Rev:: 1821                                                        $
 #==============================================================================
 #
 # ProxyConfigFilter: Customized  initialization
 #
 #=========================================================================== */

import java.net.URL;
import java.io.File;
import java.io.IOException;

import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.ucla.mbi.proxy.router.Dht;

import org.springframework.web.filter.GenericFilterBean;

//public class ProxyConfigFilter  implements Filter{
public class ProxyConfigFilter extends GenericFilterBean {
    
    private Dht dht = null;

    public void setDht ( Dht dht ) {
        this.dht = dht;
    }

    //public void init( FilterConfig config ) throws ServletException{
    public void initFilterBean() throws ServletException {    
        Log log = LogFactory.getLog( this.getClass() );
        log.info( "ProxyConfigFilter: init(config) called");
        
        String path = "";        

        try{ 
            //URL pathUrl = config.getServletContext().getResource("/");
            URL pathUrl = super.getServletContext().getResource("/");
            
            //path = new File( config.getServletContext().getRealPath("/")).getAbsolutePath();
            path = new File( super.getServletContext().getRealPath("/"))
                    .getAbsolutePath();

            log.info( " protocol=" + pathUrl.getProtocol() );
            log.info( " path=" + path );
         
            System.setProperty( "dip.proxy.home", path );
        } catch(Exception ex){ ex.printStackTrace();}


        //WSContext.getDht().initialize();
        dht.initialize();
        log.info( "after dht initialize ");

    } 
    
    public void doFilter( ServletRequest request, 
                          ServletResponse response, 
                          FilterChain chain )
        throws ServletException, IOException {
        
        chain.doFilter( request, response );        
    }
    public void destroy(){

    }
}
