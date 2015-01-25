package org.webathome.wsrest.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.webathome.wsrest.client.WsRestException;
import org.webathome.wsrest.client.RequestType;
import org.webathome.wsrest.test.support.WebUtil;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class RoundTripFixture extends FixtureBase {
    @Test
    public void simpleGet() throws Exception {
        assertEquals("OK", WebUtil.getResponse(String.format("http://localhost:%d/apis/rest/simple-ok", getPort()), null));
    }

    @Test
    public void simpleCall() throws WsRestException {
        String response = openConnection()
            .newRequest("/rest/simple-ok", RequestType.GET)
            .getText();

        assertEquals("OK", response);
    }

    @Test
    public void echoGet() throws WsRestException {
        assertEquals(
            "GET OK",
            openConnection()
                .newRequest("/rest/echo", RequestType.GET)
                .addQueryParam("value", "OK")
                .getText()
        );
    }

    @Test
    public void echoPost() throws WsRestException {
        assertEquals(
            "POST OK",
            openConnection()
                .newRequest("/rest/echo", RequestType.POST)
                .addFormParam("value", "OK")
                .getText()
        );
    }

    @Test
    public void echoPut() throws WsRestException {
        assertEquals(
            "PUT OK",
            openConnection()
                .newRequest("/rest/echo", RequestType.PUT)
                .addFormParam("value", "OK")
                .getText()
        );
    }

    @Test
    public void echoDelete() throws WsRestException {
        assertEquals(
            "DELETE OK",
            openConnection()
                .newRequest("/rest/echo", RequestType.DELETE)
                .addQueryParam("value", "OK")
                .getText()
        );
    }

    @Test
    public void echoPathParameter() throws WsRestException {
        assertEquals(
            "PATH OK",
            openConnection()
                .newRequest("/rest/echo/{value}", RequestType.GET)
                .addPathParam("value", "OK")
                .getText()
        );
    }
}
