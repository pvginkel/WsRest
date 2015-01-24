package org.webathome.wsrest.client;

public interface WebSocketCallback {
    void onClosed();

    void onStringAvailable(String value);

    void onError(Throwable e);
}
