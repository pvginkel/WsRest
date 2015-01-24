package org.webathome.wsrest.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.webathome.wsrest.client.WsRestException;
import org.webathome.wsrest.client.WsRestMethod;
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
            .newRequest(WsRestMethod.GET, "/rest/simple-ok")
            .getTextResponse();

        assertEquals("OK", response);
    }

    @Test
    public void echoGet() throws WsRestException {
        assertEquals(
            "GET OK",
            openConnection().newRequest(WsRestMethod.GET, "/rest/echo")
                .addQueryParam("value", "OK")
                .getTextResponse()
        );
    }

    @Test
    public void echoPost() throws WsRestException {
        assertEquals(
            "POST OK",
            openConnection().newRequest(WsRestMethod.POST, "/rest/echo")
                .addFormParam("value", "OK")
                .getTextResponse()
        );
    }

    @Test
    public void echoPut() throws WsRestException {
        assertEquals(
            "PUT OK",
            openConnection().newRequest(WsRestMethod.PUT, "/rest/echo")
                .addFormParam("value", "OK")
                .getTextResponse()
        );
    }

    @Test
    public void echoDelete() throws WsRestException {
        assertEquals(
            "DELETE OK",
            openConnection().newRequest(WsRestMethod.DELETE, "/rest/echo")
                .addQueryParam("value", "OK")
                .getTextResponse()
        );
    }

    @Test
    public void echoPathParameter() throws WsRestException {
        assertEquals(
            "PATH OK",
            openConnection().newRequest(WsRestMethod.GET, "/rest/echo/{value}")
                .addPathParam("value", "OK")
                .getTextResponse()
        );
    }
}
