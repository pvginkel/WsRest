package org.webathome.wsrest.client;

class PendingTextRequest implements PendingRequest {
    private final Callback<String> callback;

    public PendingTextRequest(Callback<String> callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        this.callback = callback;
    }

    @Override
    public void handleError(Throwable e) {
        callback.call(null, e);
    }

    @Override
    public StreamState handleRequest(ResponseType response, long id, String body) throws WsRestException {
        switch (response) {
            case OK:
                callback.call(body, null);

                return StreamState.CLOSED;

            default:
                throw new WsRestException("Invalid response type");
        }
    }
}
