package edu.ucla.mbi.proxy;

/*==============================================================================
 * $HeadURL::                                                                  $
 * $Id::                                                                       $
 * Version: $Rev::                                                             $
 *==============================================================================
 *
 * ProxyDxfTransformer:
 *
 *=========================================================================== */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
 
import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.context.*;
import edu.ucla.mbi.fault.*;

public class ProxyDxfTransformer {
    
    private WSContext wsContext = null;
    private String provider = null;

    public ProxyDxfTransformer( WSContext context, String provider ){
        
        this.wsContext = context;
        this.provider = provider;
    }    
    
    public DatasetType buildDxf( String strNative, String ns, String ac,
                                 String detail, // String provider, 
                                 String service ) throws ServerFault {

        // Transform native record string into DXF
        
        ProxyTransformer pTrans = wsContext
            .getServerContext( provider ).getTransformer();

        synchronized ( pTrans ) {
            pTrans.setTransformer( provider, service );
            pTrans.setParameters( detail, ns, ac );
            return pTrans.transform( strNative, detail );
        }
    }

}
