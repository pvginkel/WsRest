package org.webathome.wsrest.test;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@SuppressWarnings("UnusedDeclaration")
@Path("/rest")
public class EchoApi {
    @GET
    @Path("/simple-ok")
    @Produces(MediaType.TEXT_PLAIN)
    public String authenticate() {
        return "OK";
    }

    @GET
    @Path("/echo")
    @Produces(MediaType.TEXT_PLAIN)
    public String echoGetQuery(
        @QueryParam("value") String value
    ) {
        return "GET " + value;
    }

    @DELETE
    @Path("/echo")
    @Produces(MediaType.TEXT_PLAIN)
    public String echoDeleteQuery(
        @QueryParam("value") String value
    ) {
        return "DELETE " + value;
    }

    @POST
    @Path("/echo")
    @Produces(MediaType.TEXT_PLAIN)
    public String echoPostForm(
        @FormParam("value") String value
    ) {
        return "POST " + value;
    }

    @PUT
    @Path("/echo")
    @Produces(MediaType.TEXT_PLAIN)
    public String echoPutForm(
        @FormParam("value") String value
    ) {
        return "PUT " + value;
    }

    @GET
    @Path("/echo/{value}")
    @Produces(MediaType.TEXT_PLAIN)
    public String echoPath(
        @PathParam("value") String value
    ) {
        return "PATH " + value;
    }
}
