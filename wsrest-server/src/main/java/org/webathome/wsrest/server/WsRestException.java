package org.webathome.wsrest.server;

public class WsRestException extends Exception {
    private ErrorType type;

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

    public WsRestException(ErrorType type) {
        this.type = type;
    }

    public WsRestException(String s, ErrorType type) {
        super(s);

        this.type = type;
    }

    public WsRestException(String s, ErrorType type, Throwable throwable) {
        super(s, throwable);

        this.type = type;
    }

    public WsRestException(ErrorType type, Throwable throwable) {
        super(throwable);

        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }
}
