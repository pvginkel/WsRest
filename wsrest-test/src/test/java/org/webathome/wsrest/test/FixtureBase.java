package org.webathome.wsrest.test;

import com.koushikdutta.async.AndroidWebSocketFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.webathome.wsrest.client.WsRestConnection;
import org.webathome.wsrest.test.support.WebServer;

public abstract class FixtureBase {
    private static WebServer server;
    private WsRestConnection connection;

    protected int getPort() {
        return server.getPort();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        server = new WebServer()
            .registerServices("/apis", EchoApi.class)
            .registerWebSocketEndpoint(WsEndpoint.class)
            .start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        server.close();
    }

    protected WsRestConnection openConnection() {
        if (connection == null) {
            connection = new WsRestConnection(
                String.format("ws://localhost:%d/ws", server.getPort()),
                new AndroidWebSocketFactory()
            );
        }

        return connection;
    }

    @After
    public void afterTest() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
