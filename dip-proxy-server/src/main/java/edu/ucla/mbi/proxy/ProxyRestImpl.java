package edu.ucla.mbi.proxy;

/* =============================================================================
 # $Id::                                                                       $
 # Version: $Rev::                                                             $
 #==============================================================================
 #
 # RESTful Web service interface
 #
 #=========================================================================== */

public class ProxyRestImpl implements ProxyRest{


    //--------------------------------------------------------------------------

    public void initialize() {
        
    }

    //--------------------------------------------------------------------------

    //==========================================================================
    // REST SERVICE OPERATIONS
    //========================

    public Object getNativeRecord( String provider, String service,
                            String ns, String ac ) throws ProxyFault{

        String res = "NativeRecord: ns=" + ns + "ac=" + ac;

        ProxyServer proxyServer = new ProxyServer();

        try{

            
            ProxyServerRecord prxRec = proxyServer.getRecord( provider, service,
                                                              ns, ac, "",
                                                              "", "native",
                                                              "",  0 );
            // res = ...

        } catch( ProxyServerFault psf ){
            
            
        }
        
        return res;        
    }

    public Object getDxfRecord( String provider, String service,
                         String ns, String ac, 
                         String detail) throws ProxyFault{

        String res = "DxfRecord: ac=" + ac + " detail=" + detail;

        return res; 
    }
}
