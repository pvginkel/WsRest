package org.webathome.wsrest.test;

import com.koushikdutta.async.AndroidWebSocketFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.webathome.wsrest.client.Connection;
import org.webathome.wsrest.test.support.WebServer;

public abstract class FixtureBase {
    private static WebServer server;
    private static AndroidWebSocketFactory webSocketFactory = new AndroidWebSocketFactory();
    private Connection connection;

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

    protected Connection openConnection() {
        return openConnection(false);
    }

    protected Connection openConnection(boolean forceNew) {
        if (forceNew && connection != null) {
            connection.close();
            connection = null;
        }

        if (connection == null) {
            connection = new Connection(
                String.format("ws://localhost:%d/ws", server.getPort()),
                webSocketFactory
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
