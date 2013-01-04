package edu.ucla.mbi.proxy.struts.action;

/*==============================================================================
 * $HeadURL:: https://imex.mbi.ucla.edu/svn/dip-ws/dip-proxy/trunk/dip-proxy-s#$
 * $Id:: NativeServerConfigure.java 2812 2012-11-07 23:14:55Z lukasz           $
 * Version: $Rev:: 2812                                                        $
 *==============================================================================
 *
 * JsonContextConfigure Action:
 *
 *=========================================================================== */


import edu.ucla.mbi.util.struts.action.JsonContextConfigSupport;

public class JsonContextConfigAction extends JsonContextConfigSupport {

    private String actionName;

    public void setActionName( String name ) {
        this.actionName = name;
    }

    public String getActionName () {
        return actionName;
    }
}
    
