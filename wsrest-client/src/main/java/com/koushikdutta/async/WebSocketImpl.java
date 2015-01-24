package com.koushikdutta.async;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import org.webathome.wsrest.client.WebSocketCallback;

import java.util.LinkedList;

class WebSocketImpl implements org.webathome.wsrest.client.WebSocket {
    private final Object syncRoot = new Object();
    private WebSocket session;
    private final java.util.Deque<String> queue = new LinkedList<>();
    private final WebSocketCallback callback;
    private boolean sending;

    public WebSocketImpl(String url, WebSocketCallback callback) {
        if (url == null) {
            throw new IllegalArgumentException("url");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        this.callback = callback;

        AsyncHttpClient.getDefaultInstance().websocket(url, null, new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                WebSocketImpl.this.onCompleted(ex, webSocket);
            }
        });
    }

    private void onCompleted(Exception ex, WebSocket webSocket) {
        if (ex != null) {
            callback.onError(ex);
            return;
        }

        this.session = webSocket;

        this.session.setWriteableCallback(new WritableCallback() {
            @Override
            public void onWriteable() {
                WebSocketImpl.this.onWriteable();
            }
        });

        this.session.setStringCallback(new WebSocket.StringCallback() {
            @Override
            public void onStringAvailable(String s) {
                callback.onStringAvailable(s);
            }
        });

        this.session.setClosedCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception e) {
                callback.onClosed();
            }
        });

        beginSend();
    }

    private void onWriteable() {
        synchronized (syncRoot) {
            if (queue.size() == 0) {
                return;
            }

            sending = false;

            // Start a new run.

            beginSend();
        }
    }

    @Override
    public void sendText(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text");
        }

        synchronized (syncRoot) {
            queue.addLast(text);

            beginSend();
        }
    }

    private void beginSend() {
        if (session == null || sending || queue.size() == 0) {
            return;
        }

        session.send(queue.removeFirst());
    }

    @Override
    public void close() {
        synchronized (syncRoot) {
            if (session != null) {
                session.close();

                session = null;
            }
        }
    }
}
