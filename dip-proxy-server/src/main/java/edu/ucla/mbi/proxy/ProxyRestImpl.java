package edu.ucla.mbi.proxy;

/* =============================================================================
 # $Id::                                                                       $
 # Version: $Rev::                                                             $
 #==============================================================================
 #
 # RESTful Web service implementation
 #
 #=========================================================================== */
import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.fault.*;

import javax.xml.bind.*;
import java.io.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.xml.ws.developer.JAXWSProperties;

public class ProxyRestImpl extends ConfigurableServer implements ProxyRest{

    private Log log = LogFactory.getLog( ProxyRestImpl.class );

    //--------------------------------------------------------------------------

    public void initialize() {
        log.info( "initializing... " );
    }

    //--------------------------------------------------------------------------

    //==========================================================================
    // REST SERVICE OPERATIONS
    //========================

    public Object getNativeRecord( String provider, String service,
                            String ns, String ac ) throws ProxyFault{

        String res = "NativeRecord: ns=" + ns + "ac=" + ac;

        log.info( "res = " + res );

        ProxyServer proxyServer = new ProxyServer( wsContext );

        try{
            
            ProxyServerRecord prxRec = proxyServer.getRecord( provider, service,
                                                              ns, ac, "",
                                                              "", "native",
                                                              "",  0 );

            log.info( "prxRec native=" + 
                       prxRec.getNativeRecord().substring(0, 200 ) );

            return prxRec.getNativeRecord();

        } catch( ProxyFault psf ){
            
            
        }
        return res;        
    }

    public Object getDxfRecord( String provider, String service,
                         String ns, String ac, 
                         String detail) throws ProxyFault{

        String res = "DxfRecord: ac=" + ac + " detail=" + detail;
        
        DatasetType dataset = null;
        ProxyServer proxyServer = new ProxyServer( wsContext );

        try{

            ProxyServerRecord prxRec = proxyServer.getRecord( provider, service,
                                                              ns, ac, "",
                                                              detail, "dxf",
                                                              "",  0 );
            dataset = prxRec.getDataset();

            if( dataset != null ) {
                JAXBContext jc = DxfJAXBContext.getDxfContext();
                edu.ucla.mbi.dxf14.ObjectFactory dof =
                                new edu.ucla.mbi.dxf14.ObjectFactory();

                Marshaller marshaller = jc.createMarshaller();
                marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT,
                                        new Boolean( true ) );

                java.io.StringWriter sw = new StringWriter();
                marshaller.setProperty( Marshaller.JAXB_ENCODING, "UTF-8" );

                marshaller.marshal( dof.createDataset( dataset ), sw );

                String resultStr = sw.toString();
            
                log.info( "dxf resultStr=" + resultStr.substring( 0, 200 ) );
                return resultStr;

            }
        } catch( ProxyFault psf ){


        } catch ( Exception ex ) {

        }
        return res; 
    }
}
