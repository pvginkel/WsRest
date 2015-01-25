package org.webathome.wsrest.client;

interface PendingRequest {
    void handleError(Throwable e);

    StreamState handleRequest(ResponseType response, long id, String body) throws WsRestException;
}
