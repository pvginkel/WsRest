package org.webathome.wsrest.client;

import java.util.*;

public class WsRestConnection {
    private static final long DEFAULT_LONGER = 60 * 1000;

    private final Object syncRoot = new Object();
    private final String url;
    private final long linger;
    private final WebSocketFactory webSocketFactory;
    private long nextId = 1;
    private WebSocket webSocket;
    private final Map<Long, Callback<String>> pendingRequests = new HashMap<>();
    private Timer timer;
    private boolean closed;

    public WsRestConnection(String url, WebSocketFactory webSocketFactory) {
        this(url, DEFAULT_LONGER, webSocketFactory);
    }

    public WsRestConnection(String url, long linger, WebSocketFactory webSocketFactory) {
        if (url == null) {
            throw new IllegalArgumentException("url");
        }
        if (webSocketFactory == null) {
            throw new IllegalArgumentException("webSocketFactory");
        }

        this.url = url;
        this.linger = linger;
        this.webSocketFactory = webSocketFactory;
    }

    @SuppressWarnings("UnusedDeclaration")
    public WsRestRequest newRequest(WsRestMethod method, String path) throws WsRestException {
        if (method == null) {
            throw new IllegalArgumentException("method");
        }
        if (path == null) {
            throw new IllegalArgumentException("path");
        }

        synchronized (syncRoot) {
            if (closed) {
                throw new WsRestException("Connection closed");
            }
        }

        return new WsRestRequest(this, method, path);
    }

    void execute(WsRestMethod method, String path, String body, Callback<String> callback) throws WsRestException {
        synchronized (syncRoot) {
            if (closed) {
                throw new WsRestException("Connection closed");
            }

            long id = nextId++;
            StringBuilder sb = new StringBuilder();

            sb
                .append(method)
                .append(' ')
                .append(path)
                .append(' ')
                .append(id);

            if (body != null) {
                sb
                    .append('\n')
                    .append(body);
            }

            if (webSocket == null) {
                try {
                    webSocket = webSocketFactory.newInstance(
                        url,
                        new WebSocketCallback() {
                            @Override
                            public void onClosed() {
                                WsRestConnection.this.onClosed();
                            }

                            @Override
                            public void onStringAvailable(String value) {
                                WsRestConnection.this.onStringAvailable(value);
                            }

                            @Override
                            public void onError(Throwable e) {
                                WsRestConnection.this.onError(e);
                            }
                        }
                    );
                } catch (Exception e) {
                    throw new WsRestException("Cannot create web socket", e);
                }
            }

            // Connection will not be closed while there are pending requests.

            stopLingerTimer();

            pendingRequests.put(id, callback);

            webSocket.sendText(sb.toString());
        }
    }

    private void stopLingerTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void startLingerTimer() {
        // Reset the timer.

        stopLingerTimer();

        if (linger < 0) {
            return;
        }
        if (linger == 0) {
            webSocket.close();
            return;
        }

        timer = new Timer(true);
        timer.schedule(
            new TimerTask() {
                @Override
                public void run() {
                    webSocket.close();
                }
            },
            linger
        );
    }

    private void onClosed() {
        List<Callback<String>> callbacks;

        synchronized (syncRoot) {
            closeWebSocket();

            callbacks = new ArrayList<>(pendingRequests.values());
            pendingRequests.clear();
        }

        WsRestException e = null;

        for (Callback<String> callback : callbacks) {
            if (e == null) {
                e = new WsRestException("Connection closed unexpectedly");
            }
            callback.call(null, e);
        }
    }

    private void onStringAvailable(String value) {
        synchronized (syncRoot) {
            String header;
            String body;

            int pos = value.indexOf('\n');

            if (pos == -1) {
                header = value;
                body = null;
            } else {
                header = value.substring(0, pos);
                body = value.substring(pos + 1);
            }

            String[] parts = header.split(" ");
            if (parts.length != 2) {
                onError(new WsRestException("Protocol error"));
                return;
            }

            String response = parts[0];
            long id;
            try {
                id = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                onError(new WsRestException("Protocol error", e));
                return;
            }

            Callback<String> callback = pendingRequests.remove(id);

            if (pendingRequests.size() == 0) {
                startLingerTimer();
            }

            if (callback == null) {
                onError(new WsRestException("Unknown stream ID"));
                return;
            }

            switch (response) {
                case "OK":
                    callback.call(body, null);
                    return;

                case "ERROR":
                    String message = "Server error";
                    if (body != null) {
                        message += "\n" + body;
                    }

                    callback.call(null, new WsRestException(message));
                    break;

                default:
                    onError(new WsRestException("Unknown response type"));
                    break;
            }
        }
    }

    private void onError(Throwable e) {
        List<Callback<String>> callbacks;

        synchronized (syncRoot) {
            closeWebSocket();

            callbacks = new ArrayList<>(pendingRequests.values());
            pendingRequests.clear();
        }

        for (Callback<String> callback : callbacks) {
            callback.call(null, e);
        }
    }

    private void closeWebSocket() {
        if (webSocket != null) {
            webSocket.close();
        }

        webSocket = null;

        stopLingerTimer();
    }

    public void close() {
        synchronized (syncRoot) {
            if (!closed) {
                closeWebSocket();

                closed = true;
            }
        }
    }
}
