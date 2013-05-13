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

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import javax.xml.datatype.XMLGregorianCalendar;

public class ProxyRestImpl implements ProxyRest{

    private Log log = LogFactory.getLog( ProxyRestImpl.class );

    //--------------------------------------------------------------------------

    public void initialize() {
        log.info( "initializing... " );
    }

    //--------------------------------------------------------------------------

    private ProxyServer proxyServer;

    public void setProxyServer( ProxyServer server){
        
        proxyServer = server;
    }

    //==========================================================================
    // REST SERVICE OPERATIONS
    //========================

    public Object getNativeRecord( String provider, String service,
                                   String ns, String ac ) throws ServerFault{

        String res = "NativeRecord: ns=" + ns + "ac=" + ac;

        log.info( "res = " + res );
        
        try{
            
            ProxyServerRecord prxRec = proxyServer.getRecord( provider, service,
                                                              ns, ac, "",
                                                              "", "native",
                                                              "",  0 );

            log.info( "prxRec native=" + 
                       prxRec.getNativeRecord().substring(0, 200 ) );

            return prepareResponse( prxRec.getNativeRecord(), 
                                    prxRec.getTimestamp() );
            
        } catch( ServerFault sf ){
            log.warn( "getNativeRec catch fault mes=" + sf.getMessage() );            
            
        } catch( IOException iox){
            log.warn( "getNativeRec catch ioexception=" + iox.toString() );
        }
        return  res;        
    }

    public Object getDxfRecord( String provider, String service,
                         String ns, String ac, 
                         String detail) throws ServerFault{

        String res = "DxfRecord: ac=" + ac + " detail=" + detail;
        
        DatasetType dataset = null;
        
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
                return prepareResponse( resultStr,
                                        prxRec.getTimestamp() );

            }
        } catch( ServerFault sf ){
            log.warn( "getDxfRec catch fault mes=" + sf.getMessage() );

        } catch ( Exception ex ) {
            log.warn( "getDxfRec catch ioexception=" + ex.toString() );
        }
        return res; 
    }

    //--------------------------------------------------------------------------

    protected Response prepareResponse( String record, 
                                        XMLGregorianCalendar timestamp ) 
        throws IOException {
        
        Response.ResponseBuilder rb = Response.status( 200 );
        rb.type( "text/plain");
        rb.entity( new GenericEntity<String>( record ){} );
        rb.header( "X-PROXY-timestamp", timestamp.toString() );

        return rb.build();
    }

    //--------------------------------------------------------------------------

    protected Response prepareFaultResponse( ServerFault sf ){
        
        Response.ResponseBuilder rb = Response.status( 500 );
        rb.header( "X-PROXY-error-code", ... );
        rb.header( "X-PROXY-error-message", ... );


    }

}
