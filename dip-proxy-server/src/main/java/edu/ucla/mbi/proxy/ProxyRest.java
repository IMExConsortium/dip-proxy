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
                                @PathParam("ac") String ac );

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
                             @QueryParam("detail") String detail );

    @POST @Path("/query-native")
        @Consumes("application/json")
        Object getByPostNativeRecord( String request );

    @POST @Path("/query-dxf")
        @Consumes("application/json")
        Object getByPostDxfRecord( String request );

}
