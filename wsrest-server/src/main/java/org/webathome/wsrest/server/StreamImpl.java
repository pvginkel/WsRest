package org.webathome.wsrest.server;

import org.apache.commons.lang3.Validate;

class StreamImpl implements Stream {
    private final Object syncRoot = new Object();
    private final BufferedSession session;
    private final long id;
    private boolean closed;
    private Callback callback;

    public StreamImpl(BufferedSession session, long id) {
        Validate.notNull(session, "session");

        this.session = session;
        this.id = id;
    }

    @Override
    public void setCallback(Callback callback) {
        synchronized (syncRoot) {
            this.callback = callback;
        }
    }

    public Callback getCallback() {
        synchronized (syncRoot) {
            return callback;
        }
    }

    public boolean isClosed() {
        synchronized (syncRoot) {
            return closed;
        }
    }

    public long getId() {
        return id;
    }

    @Override
    public void sendText(String message) throws WsRestException {
        Validate.notNull(message, "message");

        synchronized (syncRoot) {
            if (closed) {
                throw new WsRestException("Stream has been closed");
            }

            Response response = new Response(
                ResponseType.MESSAGE,
                id,
                message
            );

            session.sendText(response.toString());
        }
    }

    @Override
    public void close() throws WsRestException {
        close(true);
    }

    public void close(boolean sendMessage) {
        synchronized (syncRoot) {
            if (closed) {
                return;
            }

            if (sendMessage) {
                Response response = new Response(
                    ResponseType.CLOSE,
                    id,
                    null
                );

                session.sendText(response.toString());
            }

            session.removeStream(this);

            closed = true;

            if (callback != null) {
                callback.onClosed();
            }
        }
    }
}
