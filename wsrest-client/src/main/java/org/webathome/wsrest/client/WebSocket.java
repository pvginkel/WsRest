package org.webathome.wsrest.client;

public interface WebSocket {
    void sendText(String value);

    void close();
}
