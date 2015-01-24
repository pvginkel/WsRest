package org.eclipse.jetty.websocket;

import org.webathome.wsrest.client.WebSocket;
import org.webathome.wsrest.client.WebSocketCallback;
import org.webathome.wsrest.client.WebSocketFactory;

public class JettyWebSocketFactory implements WebSocketFactory {
    @Override
    public WebSocket newInstance(String url, WebSocketCallback callback) throws Exception {
        return new WebSocketImpl(url, callback);
    }
}
