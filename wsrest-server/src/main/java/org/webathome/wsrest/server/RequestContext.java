package org.webathome.wsrest.server;

public class RequestContext {
    private static final ThreadLocal<RequestContext> CURRENT = new ThreadLocal<>();

    static void setCurrent(RequestContext context) {
        if (context == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(context);
        }
    }

    public static RequestContext getCurrent() {
        return CURRENT.get();
    }
}
