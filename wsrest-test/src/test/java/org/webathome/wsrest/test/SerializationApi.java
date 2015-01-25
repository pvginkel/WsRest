package org.webathome.wsrest.test;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@SuppressWarnings("UnusedDeclaration")
@Path("/serialization")
@Produces(MediaType.APPLICATION_FORM_URLENCODED)
public class SerializationApi {
    @GET
    @Path("/echo-char")
    public char echoChar(
        @QueryParam("value") char value
    ) {
        return value;
    }

    @GET
    @Path("/echo-nullable-char")
    public Character echoNullableChar(
        @QueryParam("value") Character value
    ) {
        return value;
    }

    @GET
    @Path("/echo-short")
    public short echoShort(
        @QueryParam("value") short value
    ) {
        return value;
    }

    @GET
    @Path("/echo-nullable-short")
    public Short echoNullableShort(
        @QueryParam("value") Short value
    ) {
        return value;
    }

    @GET
    @Path("/echo-int")
    public int echoInt(
        @QueryParam("value") int value
    ) {
        return value;
    }

    @GET
    @Path("/echo-nullable-int")
    public Integer echoNullableInt(
        @QueryParam("value") Integer value
    ) {
        return value;
    }

    @GET
    @Path("/echo-long")
    public long echoLong(
        @QueryParam("value") long value
    ) {
        return value;
    }

    @GET
    @Path("/echo-nullable-long")
    public Long echoNullableLong(
        @QueryParam("value") Long value
    ) {
        return value;
    }

    @GET
    @Path("/echo-string")
    public String echoString(
        @QueryParam("value") String value
    ) {
        return value;
    }

    @GET
    @Path("/echo-float")
    public float echoFloat(
        @QueryParam("value") float value
    ) {
        return value;
    }

    @GET
    @Path("/echo-nullable-float")
    public Float echoNullableFloat(
        @QueryParam("value") Float value
    ) {
        return value;
    }

    @GET
    @Path("/echo-double")
    public double echoDouble(
        @QueryParam("value") double value
    ) {
        return value;
    }

    @GET
    @Path("/echo-nullable-double")
    public Double echoNullableDouble(
        @QueryParam("value") Double value
    ) {
        return value;
    }

    @GET
    @Path("/echo-boolean")
    public boolean echoBoolean(
        @QueryParam("value") boolean value
    ) {
        return value;
    }

    @GET
    @Path("/echo-nullable-boolean")
    public Boolean echoNullableBoolean(
        @QueryParam("value") Boolean value
    ) {
        return value;
    }

    @GET
    @Path("/echo-multiple")
    public long echoMultiple(
        @QueryParam("a") int a,
        @QueryParam("b") short b,
        @QueryParam("c") long c
    ) {
        return a + b + c;
    }

    @GET
    @Path("/echo-int-array")
    public int echoIntArray(
        @QueryParam("value") int[] value
    ) {
        int sum = 0;
        for (int i : value) {
            sum += i;
        }
        return sum;
    }

    @GET
    @Path("/echo-object")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public TestObject echoObject(
        @FormParam("object") TestObject object
    ) {
        return object;
    }

    @GET
    @Path("/echo-array")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public TestObject[] echoArray(
        @FormParam("object") TestObject[] object
    ) {
        return object;
    }

    @GET
    @Path("/echo-list")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<TestObject> echoList(
        @FormParam("object") List<TestObject> object
    ) {
        return object;
    }

    @GET
    @Path("/echo-enum")
    public MyEnum echoEnum(
        @QueryParam("value") MyEnum value
    ) {
        return value;
    }
}
