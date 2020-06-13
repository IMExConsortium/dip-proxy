package edu.ucla.mbi.proxy;

/* =============================================================================
 # $Id::                                                                       $
 # Version: $Rev::                                                             $
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

import edu.ucla.mbi.proxy.context.WSContext;
import edu.ucla.mbi.fault.ServerFault;

public class ProxyConfigFilter implements Filter{

    public void init( FilterConfig config ) throws ServletException{
        Log log = LogFactory.getLog( this.getClass() );
        log.info( "ProxyConfigFilter: init(config) called");
        
        String path = "";        

        try{ 
            URL pathUrl = config.getServletContext().getResource("/");
            
            path = new File( config.getServletContext().getRealPath("/") )
                .getAbsolutePath();

            log.info( " protocol=" + pathUrl.getProtocol() );
            log.info( " path=" + path );
            
            System.setProperty( "dip.proxy.home", path );

        } catch(Exception ex){
            ex.printStackTrace();
        }
        
    } 
    
    public void doFilter( ServletRequest request, 
                          ServletResponse response, 
                          FilterChain chain )
        throws ServletException, IOException {
        System.out.println("ProxyConfigFilter.doFilter");
        chain.doFilter( request, response );        
    }
    public void destroy(){

    }
}
