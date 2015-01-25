package org.webathome.wsrest.server;

public interface Stream {
    void setCallback(Callback callback);

    void sendText(String message) throws WsRestException;

    void close() throws WsRestException;

    public static interface Callback {
        void onMessage(String message);

        void onClosed();

        void onError(Throwable e);
    }
}
