package org.eclipse.jetty.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.common.io.FutureWriteCallback;
import org.webathome.wsrest.client.WebSocketCallback;
import org.webathome.wsrest.client.WsRestException;

import java.net.URI;
import java.util.LinkedList;

class WebSocketImpl implements org.webathome.wsrest.client.WebSocket {
    private final Object syncRoot = new Object();
    private final Socket socket;
    private final java.util.Deque<String> queue = new LinkedList<>();
    private final WebSocketCallback callback;
    private WebSocketClient client;
    private boolean sending;

    public WebSocketImpl(String url, WebSocketCallback callback) throws Exception {
        if (url == null) {
            throw new IllegalArgumentException("url");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        this.callback = callback;

        client = new WebSocketClient();
        client.start();

        socket = new Socket();

        client.connect(
            socket,
            new URI(url),
            new ClientUpgradeRequest()
        );
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
        if (socket.session == null || sending || queue.size() == 0) {
            return;
        }

        socket.session.getRemote().sendString(
            queue.removeFirst(),
            new FutureWriteCallback() {
                @Override
                public void writeFailed(Throwable cause) {
                    callback.onError(cause);
                }

                @Override
                public void writeSuccess() {
                    synchronized (syncRoot) {
                        if (queue.size() == 0) {
                            return;
                        }

                        sending = false;

                        // Start a new run.

                        beginSend();
                    }
                }
            }
        );
    }

    @Override
    public void close() {
        synchronized (syncRoot) {
            if (client != null) {
                try {
                    client.stop();
                } catch (Exception e) {
                    // Ignore.
                }

                client = null;
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @WebSocket
    public class Socket {
        private Session session;

        @OnWebSocketClose
        public void onClose(int statusCode, String reason) {
            synchronized (syncRoot) {
                if (statusCode != StatusCode.NORMAL) {
                    callback.onError(new WsRestException(String.format("Unexpected error %s", reason)));
                } else {
                    callback.onClosed();
                }
            }
        }

        @OnWebSocketConnect
        public void onConnect(Session session) {
            synchronized (syncRoot) {
                this.session = session;

                beginSend();
            }
        }

        @OnWebSocketMessage
        public void onMessage(String msg) {
            callback.onStringAvailable(msg);
        }

        @OnWebSocketError
        public void onError(Throwable e) {
            callback.onError(e);
        }
    }
}
