package org.webathome.wsrest.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.webathome.wsrest.client.WsRestException;
import org.webathome.wsrest.client.WsRestMethod;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class SerializationFixture extends FixtureBase {
    @Test
    public void echoChar() throws WsRestException {
        test("char", 'a');
    }

    @Test
    public void echoNullableChar() throws WsRestException {
        test("nullable-char", 'a');
    }

    @Test
    public void echoShort() throws WsRestException {
        test("short", (short)42);
    }

    @Test
    public void echoNullableShort() throws WsRestException {
        test("nullable-short", (short)42);
    }

    @Test
    public void echoInt() throws WsRestException {
        test("int", 42);
    }

    @Test
    public void echoNullableInt() throws WsRestException {
        test("nullable-int", 42);
    }

    @Test
    public void echoLong() throws WsRestException {
        test("long", (long)42);
    }

    @Test
    public void echoNullableLong() throws WsRestException {
        test("nullable-long", (long)42);
    }

    @Test
    public void echoString() throws WsRestException {
        test("string", "Hello world!");
    }

    @Test
    public void echoFloat() throws WsRestException {
        test("float", (float)42.7);
    }

    @Test
    public void echoNullableFloat() throws WsRestException {
        test("nullable-float", (float)42.7);
    }

    @Test
    public void echoDouble() throws WsRestException {
        test("double", 42.7);
    }

    @Test
    public void echoNullableDouble() throws WsRestException {
        test("nullable-double", 42.7);
    }

    @Test
    public void echoBoolean() throws WsRestException {
        test("boolean", true);
    }

    @Test
    public void echoNullableBoolean() throws WsRestException {
        test("nullable-boolean", true);
    }

    @Test
    public void echoIntArray() throws WsRestException {

        assertEquals(
            1 + 2 + 3,
            (int)openConnection().newRequest(WsRestMethod.GET, "/serialization/echo-int-array")
                .addQueryParam("value", new int[]{1, 2, 3})
                .getResponse(Integer.class)
        );
    }

    @Test
    public void echoNull() throws WsRestException {
        assertEquals(
            null,
            openConnection().newRequest(WsRestMethod.GET, "/serialization/echo-nullable-int")
                .addQueryParam("value", null)
                .getResponse(Integer.class)
        );
    }

    @Test
    public void echoDefaultInt() throws WsRestException {
        assertEquals(
            0,
            (int)openConnection().newRequest(WsRestMethod.GET, "/serialization/echo-int")
                .getResponse(Integer.class)
        );
    }

    @Test
    public void echoMultiple() throws WsRestException {
        assertEquals(
            (long)(42 + 84 + 168),
            (long)openConnection().newRequest(WsRestMethod.GET, "/serialization/echo-multiple")
                .addQueryParam("a", 42)
                .addQueryParam("b", 84)
                .addQueryParam("c", 168)
                .getResponse(Long.class)
        );
    }

    @Test
    public void echoObject() throws WsRestException {
        TestObject testObject = new TestObject();
        testObject.setA("Hello world!");
        testObject.setB(42);

        assertEquals(
            testObject,
            openConnection().newRequest(WsRestMethod.GET, "/serialization/echo-object")
                .setJsonBody(testObject)
                .getJsonResponse(TestObject.class)
        );
    }

    @Test
    public void echoArray() throws WsRestException {
        TestObject testObject1 = new TestObject();
        testObject1.setA("Hello world 1!");
        testObject1.setB(42);
        TestObject testObject2 = new TestObject();
        testObject2.setA("Hello world 2!");
        testObject2.setB(42);
        TestObject[] array = new TestObject[]{testObject1, testObject2};

        assertArrayEquals(
            array,
            (TestObject[])openConnection().newRequest(WsRestMethod.GET, "/serialization/echo-array")
                .setJsonBody(array)
                .getJsonResponse(TestObject[].class)
        );
    }

    @Test
    public void echoList() throws WsRestException {
        List<TestObject> list = new ArrayList<>();
        TestObject testObject1 = new TestObject();
        testObject1.setA("Hello world 1!");
        testObject1.setB(42);
        list.add(testObject1);
        TestObject testObject2 = new TestObject();
        testObject2.setA("Hello world 2!");
        testObject2.setB(42);
        list.add(testObject2);

        assertEquals(
            list,
            openConnection().newRequest(WsRestMethod.GET, "/serialization/echo-list")
                .setJsonBody(list)
                .getJsonResponse(TestObject.LIST_TYPE)
        );
    }

    private void test(String method, Object value) throws WsRestException {
        assertEquals(
            value,
            openConnection().newRequest(WsRestMethod.GET, "/serialization/echo-" + method)
                .addQueryParam("value", value)
                .getResponse(value.getClass())
        );
    }
}
