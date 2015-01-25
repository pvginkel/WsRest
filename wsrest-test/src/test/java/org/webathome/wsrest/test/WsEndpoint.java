package org.webathome.wsrest.test;

import org.webathome.wsrest.server.AbstractWsEndpoint;
import org.webathome.wsrest.server.WsRestContext;
import org.webathome.wsrest.server.WsRestException;

import javax.websocket.ClientEndpoint;
import javax.websocket.server.ServerEndpoint;

@ClientEndpoint
@ServerEndpoint("/ws")
public class WsEndpoint extends AbstractWsEndpoint {
    private static final WsRestContext CONTEXT = getBuild();

    private static WsRestContext getBuild() {
        try {
            return new WsRestContext.Builder()
                .addService(EchoApi.class)
                .addService(SerializationApi.class)
                .addService(StreamApi.class)
                .build();
        } catch (WsRestException e) {
            throw new IllegalStateException(e);
        }
    }

    public WsEndpoint() {
        super(CONTEXT);
    }
}
