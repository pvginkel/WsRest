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

    private WsRestContext(List<Class<?>> services, ExecutorService threadPool) throws WsRestException {
        for (Class<?> service : services) {
            endpoints.add(new EndpointDescription(service));
        }

        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        }

        this.threadPool = threadPool;
    }

    void execute(final String message, final BufferedSession session) {
        Validate.notNull(message, "message");
        Validate.notNull(session, "session");

        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                Response response = execute(message);

                session.sendText(response.toString());
            }
        });
    }

    private Response execute(String message) {
        long id = -1;

        try {
            Request request = Request.parse(message);
            id = request.getId();

            for (EndpointDescription endpoint : endpoints) {
                if (request.getPath().startsWith(endpoint.getPath())) {
                    return endpoint.execute(request);
                }
            }

            throw new WsRestException("Cannot find endpoint", ErrorType.NOT_FOUND);
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

    public static class Builder {
        private final List<Class<?>> services = new ArrayList<>();
        private ExecutorService threadPool;

        public Builder addService(Class<?> service) {
            Validate.notNull(service, "service");

            services.add(service);

            return this;
        }

        public Builder setThreadPool(ExecutorService threadPool) {
            this.threadPool = threadPool;
            return this;
        }

        public WsRestContext build() throws WsRestException {
            return new WsRestContext(services, threadPool);
        }
    }
}
