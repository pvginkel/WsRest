package org.webathome.wsrest.client;

class PendingStreamRequest implements PendingRequest {
    private final Callback<Stream> callback;
    private final Connection connection;
    private StreamImpl stream;
    private long id;

    public PendingStreamRequest(Callback<Stream> callback, Connection connection) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }
        if (connection == null) {
            throw new IllegalArgumentException("connection");
        }

        this.callback = callback;
        this.connection = connection;
    }

    public long getId() {
        return id;
    }

    @Override
    public void handleError(Throwable e) {
        callback.call(null, e);
    }

    @Override
    public StreamState handleRequest(ResponseType response, long id, String body) throws WsRestException {
        this.id = id;

        switch (response) {
            case OPEN:
                if (stream != null) {
                    throw new WsRestException("Protocol error");
                }

                createStream(id);

                return StreamState.PENDING;

            case CLOSE:
                if (stream == null) {
                    createStream(id);
                }

                stream.close(false);

                return StreamState.CLOSED;

            case MESSAGE:
                stream.onMessage(body);

                return StreamState.PENDING;

            default:
                throw new WsRestException("Invalid response type");
        }
    }

    private void createStream(long id) {
        stream = new StreamImpl(connection, id, this);

        callback.call(stream, null);
    }
}
