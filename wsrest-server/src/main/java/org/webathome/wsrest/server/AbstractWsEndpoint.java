package org.webathome.wsrest.server;

import org.apache.commons.lang3.Validate;

import javax.websocket.*;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWsEndpoint {
    private final WsRestContext context;
    private final Object syncRoot = new Object();
    private final Map<Session, BufferedSession> sessions = new HashMap<>();

    protected AbstractWsEndpoint(WsRestContext context) {
        Validate.notNull(context, "context");

        this.context = context;
    }

    @OnOpen
    public void onOpen(Session session) {
        synchronized (syncRoot) {
            sessions.put(session, new BufferedSession(session));
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        BufferedSession bufferedSession;

        synchronized (syncRoot) {
            bufferedSession = sessions.remove(session);
        }

        if (bufferedSession != null) {
            bufferedSession.close(null);
        }
    }

    @OnError
    public void onError(Session session, Throwable e) {
        BufferedSession bufferedSession;

        synchronized (syncRoot) {
            bufferedSession = sessions.remove(session);
        }

        if (bufferedSession != null) {
            bufferedSession.close(e);
        }
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        BufferedSession bufferedSession;

        synchronized (syncRoot) {
            bufferedSession = sessions.get(session);
        }

        if (bufferedSession != null) {
            context.execute(message, bufferedSession);
        }
    }
}
