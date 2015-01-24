package org.webathome.wsrest.client;

public interface WebSocketFactory {
    WebSocket newInstance(String url, WebSocketCallback callback) throws Exception;
}
