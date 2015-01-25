package org.webathome.wsrest.client;

import java.util.ArrayList;
import java.util.List;

class StreamImpl implements Stream {
    private final Connection connection;
    private final long id;
    private final PendingStreamRequest request;
    private Callback callback;
    private boolean closed;
    private List<String> queue = new ArrayList<>();

    public StreamImpl(Connection connection, long id, PendingStreamRequest request) {
        if (connection == null) {
            throw new IllegalArgumentException("connection");
        }
        if (request == null) {
            throw new IllegalArgumentException("request");
        }

        this.connection = connection;
        this.id = id;
        this.request = request;
    }

    @Override
    public void setCallback(Callback callback) {
        synchronized (connection.getSyncRoot()) {
            this.callback = callback;

            if (callback == null) {
                if (queue == null) {
                    queue = new ArrayList<>();
                }
            } else if (queue != null) {
                for (String message : queue) {
                    callback.onMessage(message);
                }

                queue = null;
            }
        }
    }

    public void onMessage(String message) {
        synchronized (connection.getSyncRoot()) {
            if (queue != null) {
                queue.add(message);
            } else {
                callback.onMessage(message);
            }
        }
    }

    public Callback getCallback() {
        synchronized (connection.getSyncRoot()) {
            return callback;
        }
    }

    @Override
    public void sendText(String message) throws WsRestException {
        synchronized (connection.getSyncRoot()) {
            if (closed) {
                throw new WsRestException("Stream has been closed");
            }
        }

        connection.execute(
            RequestType.MESSAGE,
            "~",
            message,
            null,
            id
        );
    }

    @Override
    public void close() throws WsRestException {
        close(true);
    }

    public void close(boolean sendMessage) throws WsRestException {
        synchronized (connection.getSyncRoot()) {
            if (closed) {
                return;
            }

            if (sendMessage) {
                connection.execute(RequestType.CLOSE, "~", null, null, id);
            }

            connection.removeRequest(request);

            closed = true;

            if (callback != null) {
                callback.onClosed();
            }
        }
    }
}
