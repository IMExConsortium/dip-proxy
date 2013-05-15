package edu.ucla.mbi.proxy;

/* =============================================================================
 # $Id::                                                                       $
 # Version: $Rev::                                                             $
 #==============================================================================
 #
 # RESTful Web service interface
 #
 #=========================================================================== */

import javax.ws.rs.*;
import edu.ucla.mbi.fault.*;

public interface ProxyRest {

    @GET @Path("/native-record/{provider}/{service}/{ns}/{ac}")
        Object getNativeRecord( @DefaultValue("")
                                @PathParam("provider") String provider,
                                @DefaultValue("")
                                @PathParam("service") String service,
                                @DefaultValue("")
                                @PathParam("ns") String ns,
                                @DefaultValue("")
                                @PathParam("ac") String ac ) 
        throws ServerFault;

    @GET @Path("/dxf-record/{provider}/{service}/{ns}/{ac}")
        Object getDxfRecord( @DefaultValue("")
                             @PathParam("provider") String provider,
                             @DefaultValue("")
                             @PathParam("service") String service,
                             @DefaultValue("")
                             @PathParam("ns") String ns,
                             @DefaultValue("")
                             @PathParam("ac") String ac,
                             @DefaultValue("base")
                             @QueryParam("detail") String detail ) 
        throws ServerFault;

    @POST @Path("/query-native")
        @Consumes("application/json")
        Object getByPostNativeRecord( String request ) 
        throws ServerFault;

    @POST @Path("/query-dxf")
        @Consumes("application/json")
        Object getByPostDxfRecord( String request )
        throws ServerFault;

    
    /*
    @GET @Path("/interaction/{interactionAc}")
        Object getByInteraction( @PathParam("interactionAc") String intAc,
                                 @DefaultValue("") 
                                 @QueryParam("db") String db,
                                 @DefaultValue("tab25") 
                                 @QueryParam("format") String format,
                                 @DefaultValue("0") 
                                 @QueryParam("firstResult") String firstResult,
                                 @DefaultValue("500") 
                                 @QueryParam("maxResults") String maxResults ) 
        throws PsicquicServiceException,
               NotSupportedMethodException,
               NotSupportedTypeException;

    @GET @Path("/query/{query}")
        Object getByQuery( @PathParam("query") String query,
                           @DefaultValue("tab25") 
                           @QueryParam("format") String format,
                           @DefaultValue("0") 
                           @QueryParam("firstResult") String firstResult,
                           @DefaultValue("500") 
                           @QueryParam("maxResults") String maxResults ) 
        throws PsicquicServiceException,
               NotSupportedMethodException,
               NotSupportedTypeException;

    @POST @Path("/query")
        @Consumes("application/json")
        Object getByPostQuery( String request) 
        throws PsicquicServiceException,
               NotSupportedMethodException,
               NotSupportedTypeException;

    @GET @Path("/formats")
        Object getSupportedFormats() throws PsicquicServiceException,
                                            NotSupportedMethodException;
    
    @GET @Path("/property/{propertyName}")
        Object getProperty( @PathParam("propertyName") 
                            String propertyName )
        throws PsicquicServiceException,
               NotSupportedMethodException;
    
    @GET @Path("/properties")
        Object getProperties() throws PsicquicServiceException,
                                      NotSupportedMethodException;
    
    @GET @Path("/version")
        String getVersion() throws PsicquicServiceException,
                                   NotSupportedMethodException;
    */
}
