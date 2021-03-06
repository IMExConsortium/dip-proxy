package edu.ucla.mbi.proxy;

/* =============================================================================
 #                                                                             $
 # RESTful Web service implementation                                          $
 #                                                                             $
 #=========================================================================== */

import edu.ucla.mbi.dxf14.*;
import edu.ucla.mbi.proxy.*;
import edu.ucla.mbi.fault.*;

import org.json.*;

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
                                   String ns, String ac ){

        String res = "NativeRecord: ns=" + ns + "ac=" + ac;

        log.info( "res = " + res );
        
        try{
            
            ProxyServerRecord prxRec = proxyServer.getRecord( provider, service,
                                                              ns, ac, "",
                                                              "", "native",
                                                              "",  0 );

            log.info( "prxRec native=" + 
                       prxRec.getNativeRecord().substring(0, 200 ) );

            if( prxRec.getNativeRecord() != null ) {
                return prepareResponse( prxRec.getNativeRecord(), 
                                        prxRec.getTimestamp() );
            } else {
                return prepareFaultResponse(
                    ServerFaultFactory.newInstance( Fault.UNKNOWN ) );
            }
        } catch( ServerFault sf ){
            log.warn( "getNativeRec catch fault mes=" + sf.getMessage() );            
            return prepareFaultResponse( sf );
            
        } catch( IOException iox){
            log.warn( "getNativeRec catch ioexception=" + iox.toString() );
            return prepareFaultResponse(
                ServerFaultFactory.newInstance( Fault.UNKNOWN ) );

        }
    }

    public Object getDxfRecord( String provider, String service,
                                String ns, String ac, 
                                String detail){

        String res = "DxfRecord: ac=" + ac + " detail=" + detail;
        log.info( "res=" + res );    

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

            } else {
                return prepareFaultResponse(
                    ServerFaultFactory.newInstance( Fault.UNKNOWN ) );
            }
        } catch( ServerFault sf ){
            log.warn( "getDxfRec catch fault mes=" + sf.getMessage() );
            return prepareFaultResponse( sf );

        } catch ( Exception ex ) {
            log.warn( "getDxfRec catch ioexception=" + ex.toString() );
            return prepareFaultResponse( 
                ServerFaultFactory.newInstance( Fault.UNKNOWN ) );

        }
    }

    public Object getByPostNativeRecord( String request ){

        JSONObject jRequest = null;

        try {
            jRequest = requestValidation ( request );
        } catch ( ServerFault sf ) {
            return prepareFaultResponse( sf );
        }

        try {
            return getNativeRecord( jRequest.getString( "provider" ), 
                                    jRequest.getString( "service" ), 
                                    jRequest.getString( "ns" ), 
                                    jRequest.getString( "ac" ) );
        } catch ( JSONException jx ){
            return prepareFaultResponse(
                ServerFaultFactory.newInstance( Fault.FORMAT ) );
        }
    }

    public Object getByPostDxfRecord( String request){

        JSONObject jRequest = null;
        String detail = null;

        try {
            jRequest = requestValidation ( request );
        } catch ( ServerFault sf ) {
            return prepareFaultResponse( sf );
        }

        try {
            detail = jRequest.getString( "detail" );
        } catch ( JSONException jx ){
            detail = "base";
        }

        if ( detail == null || detail.isEmpty() ) {
            detail = "base"; 
        }

        try {
            return getDxfRecord( jRequest.getString( "provider" ),
                                 jRequest.getString( "service" ),
                                 jRequest.getString( "ns" ),
                                 jRequest.getString( "ac" ),
                                 detail );
        } catch ( JSONException jx ){
            return prepareFaultResponse(
                ServerFaultFactory.newInstance( Fault.FORMAT ) );
        }
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
        rb.header( "X-PROXY-error-code", sf.getFaultCode());
        rb.header( "X-PROXY-error-message", sf.getMessage() );
        
        return rb.build();
    }

    private JSONObject  requestValidation ( String request )
        throws ServerFault {

        JSONObject jRequest = null;

        try{
            jRequest = new JSONObject( request );
        } catch( JSONException jx ){
            throw ServerFaultFactory.newInstance( Fault.FORMAT );
        }

        if( jRequest == null ){
            throw ServerFaultFactory.newInstance( Fault.FORMAT );
        }

        jsonValidation( jRequest, "provider" );
        jsonValidation( jRequest, "service" );
        jsonValidation( jRequest, "ns" );
        jsonValidation( jRequest, "ac" );
        
        return jRequest;
    }

    private void jsonValidation ( JSONObject jRequest, String key )
        throws ServerFault {
        
        String value = null;

        try{
            value = jRequest.getString( key );
        } catch( JSONException jx ){
            throw ServerFaultFactory.newInstance( Fault.FORMAT );
        }

        if( value == null || value.isEmpty() ) {
            if( key.equals( "provider" ) || key.equals( "service" ) ) {
                throw ServerFaultFactory.newInstance( Fault.UNSUPPORTED_OP );
            } else {
                throw ServerFaultFactory.newInstance( Fault.INVALID_ID );
            }
        } 

        return;
    }
}
