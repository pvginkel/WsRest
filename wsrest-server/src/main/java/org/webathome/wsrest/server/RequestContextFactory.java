package org.webathome.wsrest.server;

import javax.websocket.Session;

public interface RequestContextFactory {
    public RequestContext createContext(Session session);
}
