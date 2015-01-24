package org.webathome.wsrest.server;

import org.apache.commons.lang3.Validate;

import javax.websocket.SendHandler;
import javax.websocket.SendResult;
import javax.websocket.Session;
import java.util.Deque;
import java.util.LinkedList;

class BufferedSession {
    private final Object syncRoot = new Object();
    private final Session session;
    private final Deque<String> queue = new LinkedList<>();
    private boolean sending;

    public BufferedSession(Session session) {
        this.session = session;
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
}