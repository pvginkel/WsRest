package org.webathome.wsrest.server;

import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WsRestContext {
    private static final int DEFAULT_THREAD_COUNT = 5;

    private final List<EndpointDescription> endpoints = new ArrayList<>();
    private final ExecutorService threadPool;
    private final RequestContextFactory requestContextFactory;

    private WsRestContext(List<Class<?>> services, ExecutorService threadPool, RequestContextFactory requestContextFactory) throws WsRestException {
        for (Class<?> service : services) {
            endpoints.add(new EndpointDescription(service));
        }

        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        }

        this.threadPool = threadPool;
        this.requestContextFactory = requestContextFactory;
    }

    RequestContextFactory getRequestContextFactory() {
        return requestContextFactory;
    }

    void execute(final String message, final BufferedSession session) {
        Validate.notNull(message, "message");
        Validate.notNull(session, "session");

        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                Response response = executeAsync(message, session);

                if (response != null) {
                    session.sendText(response.toString());
                }
            }
        });
    }

    private Response executeAsync(String message, BufferedSession session) {
        long id = -1;

        try {
            Request request = Request.parse(message);
            id = request.getId();

            switch (request.getType()) {
                case MESSAGE:
                case CLOSE:
                    return executeStreamMessage(request, session);

                default:
                    return executeNormalMessage(request, session);
            }
        } catch (Throwable e) {
            String error;

            try (
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)
            ) {
                e.printStackTrace(pw);
                error = sw.toString();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }

            return new Response(
                ResponseType.ERROR,
                id,
                error
            );
        }
    }

    private Response executeStreamMessage(Request request, BufferedSession session) throws WsRestException {
        StreamImpl stream = session.getStream(request.getId());

        if (request.getType() == RequestType.MESSAGE) {
            if (stream == null) {
                throw new WsRestException("Cannot find stream");
            }

            Stream.Callback callback = stream.getCallback();
            if (callback != null) {
                callback.onMessage(request.getBody());
            }
        } else if (stream != null) {
            stream.close(false);
        }

        return null;
    }

    private Response executeNormalMessage(Request request, BufferedSession session) throws WsRestException {
        for (EndpointDescription endpoint : endpoints) {
            if (request.getPath().startsWith(endpoint.getPath())) {
                return endpoint.execute(request, session);
            }
        }

        throw new WsRestException("Cannot find endpoint", ErrorType.NOT_FOUND);
    }

    public static class Builder {
        private final List<Class<?>> services = new ArrayList<>();
        private ExecutorService threadPool;
        private RequestContextFactory requestContextFactory;

        public Builder addService(Class<?> service) {
            Validate.notNull(service, "service");

            services.add(service);

            return this;
        }

        public Builder setThreadPool(ExecutorService threadPool) {
            this.threadPool = threadPool;
            return this;
        }

        public Builder setRequestContextFactory(RequestContextFactory requestContextFactory) {
            this.requestContextFactory = requestContextFactory;
            return this;
        }

        public WsRestContext build() throws WsRestException {
            return new WsRestContext(services, threadPool, requestContextFactory);
        }
    }
}
