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

import edu.ucla.mbi.util.context.*;
import edu.ucla.mbi.util.struts.action.JsonContextConfigSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class JsonContextConfigAction extends JsonContextConfigSupport { 

    private ContextListener contextListener;
    private JsonContext jsonContext;

    //*** setter
    public void setContextListener ( ContextListener listener ) {
        this.contextListener = listener;
    }

    public void setJsonContext ( JsonContext jsonContext ) {
        jsonContext.addContextUpdateListener( contextListener );
        super.setJsonContext( jsonContext );
    }

}
    
