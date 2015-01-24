package org.webathome.wsrest.client;

public class WsRestException extends Exception {
    public WsRestException() {
    }

    public WsRestException(String s) {
        super(s);
    }

    public WsRestException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public WsRestException(Throwable throwable) {
        super(throwable);
    }

    public WsRestException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}
