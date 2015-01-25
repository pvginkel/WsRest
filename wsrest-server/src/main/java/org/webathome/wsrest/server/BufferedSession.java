package org.webathome.wsrest.server;

import org.apache.commons.lang3.Validate;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class BufferedSession {
    private final Object syncRoot = new Object();
    private final Session session;
    private final Deque<String> queue = new LinkedList<>();
    private boolean sending;
    private final Map<Long, StreamImpl> streams = new HashMap<>();
    private final RequestContext requestContext;

    public BufferedSession(Session session, RequestContext requestContext) {
        Validate.notNull(session, "session");

        this.session = session;
        this.requestContext = requestContext;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public void sendText(String text) {
        Validate.notNull(text, "text");

        synchronized (syncRoot) {
            queue.addLast(text);

            beginSend();
        }
    }

    private void beginSend() {
        if (sending || queue.size() == 0) {
            return;
        }

        final String pending = queue.getFirst();

        session.getAsyncRemote().sendText(pending, new SendHandler() {
            @Override
            public void onResult(SendResult sendResult) {
                onSendResult(pending, sendResult);
            }
        });
    }

    public void registerStream(StreamImpl stream) {
        Validate.notNull(stream, "stream");

        synchronized (syncRoot) {
            streams.put(stream.getId(), stream);
        }
    }

    public StreamImpl getStream(long id) {
        synchronized (syncRoot) {
            return streams.get(id);
        }
    }

    public void removeStream(StreamImpl stream) {
        Validate.notNull(stream, "stream");

        synchronized (syncRoot) {
            streams.remove(stream.getId());
        }
    }

    private void onSendResult(String pending, SendResult sendResult) {
        synchronized (syncRoot) {
            // We specifically compare for referential equality because we're checking whether the pending
            // message is what we've last send. This concerns the message, not the contents!
            //noinspection StringEquality
            if (queue.size() == 0 || pending != queue.getFirst()) {
                // LOG.warn("Finished sending a message, but the head of the queue wasn't what we were sending");
                return;
            }

            if (sendResult.isOK()) {
                sending = false;

                queue.removeFirst();

                // Start a new run.

                beginSend();
            } else {
                // LOG.warn("Exception while sending a message", sendResult.getException());

                // Note sure what to do. Let's kill the session. The other side will be informed of
                // this and should initiate a new session. Because we leave the pending message
                // at the head of the queue, it will be picked up in the next try.

                try {
                    this.session.close();
                } catch (Throwable e) {
                    // LOG.warn("Could not close existing session", e);
                }
            }
        }
    }

    public void close(Throwable e) {
        synchronized (syncRoot) {
            for (StreamImpl stream : streams.values()) {
                if (e != null) {
                    Stream.Callback callback = stream.getCallback();
                    if (callback != null) {
                        callback.onError(e);
                    }
                }

                stream.close(false);
            }

            streams.clear();
        }
    }
}
