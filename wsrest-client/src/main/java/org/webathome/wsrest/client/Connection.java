package org.webathome.wsrest.client;

import java.util.*;

public class Connection {
    private static final long DEFAULT_LONGER = 60 * 1000;

    private final Object syncRoot = new Object();
    private final String url;
    private final long linger;
    private final WebSocketFactory webSocketFactory;
    private long nextId = 1;
    private WebSocket webSocket;
    private final Map<Long, PendingRequest> pendingRequests = new HashMap<>();
    private Timer timer;
    private boolean closed;

    public Connection(String url, WebSocketFactory webSocketFactory) {
        this(url, DEFAULT_LONGER, webSocketFactory);
    }

    public Connection(String url, long linger, WebSocketFactory webSocketFactory) {
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

    Object getSyncRoot() {
        return syncRoot;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Request newRequest(String path, RequestType method) throws WsRestException {
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

        return new Request(this, method, path);
    }

    void execute(RequestType method, String path, String body, PendingRequest request) throws WsRestException {
        synchronized (syncRoot) {
            if (closed) {
                throw new WsRestException("Connection closed");
            }

            execute(method, path, body, request, nextId++);
        }
    }

    void execute(RequestType method, String path, String body, PendingRequest request, long id) throws WsRestException {
        synchronized (syncRoot) {
            if (closed) {
                throw new WsRestException("Connection closed");
            }

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
                                Connection.this.onClosed();
                            }

                            @Override
                            public void onStringAvailable(String value) {
                                Connection.this.onStringAvailable(value);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Connection.this.onError(e);
                            }
                        }
                    );
                } catch (Exception e) {
                    throw new WsRestException("Cannot create web socket", e);
                }
            }

            if (request != null) {
                pendingRequests.put(id, request);
            }

            // Connection will not be closed while there are pending requests.

            if (pendingRequests.size() > 0) {
                stopLingerTimer();
            } else {
                startLingerTimer();
            }

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
        List<PendingRequest> requests;

        synchronized (syncRoot) {
            closeWebSocket();

            requests = new ArrayList<>(pendingRequests.values());
            pendingRequests.clear();
        }

        WsRestException e = null;

        for (PendingRequest request : requests) {
            if (e == null) {
                e = new WsRestException("Connection closed unexpectedly");
            }
            request.handleError(e);
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

            ResponseType response;
            long id;
            try {
                response = ResponseType.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                onError(new WsRestException("Protocol error", e));
                return;
            }
            try {
                id = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                onError(new WsRestException("Protocol error", e));
                return;
            }

            if (response == ResponseType.ERROR) {
                PendingRequest request = pendingRequests.remove(id);

                if (request == null) {
                    onError(new WsRestException("Unknown stream ID"));
                    return;
                }

                String message = "Server error";
                if (body != null) {
                    message += "\n" + body;
                }

                request.handleError(new WsRestException(message));
            } else {
                PendingRequest request = pendingRequests.get(id);

                if (request == null) {
                    onError(new WsRestException("Unknown stream ID"));
                    return;
                }

                StreamState state;
                try {
                    state = request.handleRequest(response, id, body);
                } catch (WsRestException e) {
                    onError(e);
                    return;
                }

                if (state == StreamState.CLOSED) {
                    pendingRequests.remove(id);
                }
            }

            if (pendingRequests.size() == 0) {
                startLingerTimer();
            }
        }
    }

    private void onError(Throwable e) {
        List<PendingRequest> requests;

        synchronized (syncRoot) {
            closeWebSocket();

            requests = new ArrayList<>(pendingRequests.values());
            pendingRequests.clear();
        }

        for (PendingRequest request : requests) {
            request.handleError(e);
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

    public void removeRequest(PendingStreamRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request");
        }

        this.pendingRequests.remove(request.getId());
    }
}
