package org.webathome.wsrest.test.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.http.spi.JettyHttpServer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import java.util.EnumSet;

public class WebServer implements AutoCloseable {
    private static final EnumSet<DispatcherType> DISPATCHER_TYPE_ALL = EnumSet.of(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.REQUEST);

    private Server server;
    private ContextHandlerCollection contexts;
    private JettyHttpServer jettyServer;
    private ServletContextHandler handler;
    private ServerConnector connector;

    public int getPort() {
        return connector.getLocalPort();
    }

    public WebServer() {
        createServer();
    }

    @Override
    public void close() throws Exception {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private void createServer() {
        server = new Server();

        HttpConfiguration httpsConfiguration = new HttpConfiguration();

        httpsConfiguration.addCustomizer(new SecureRequestCustomizer());

        connector = new ServerConnector(
            server,
            new HttpConnectionFactory(httpsConfiguration)
        );

        server.addConnector(connector);

        jettyServer = new JettyHttpServer(server, true);

        contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        handler = new ServletContextHandler(contexts, "/", true, false);
        handler.addFilter(GzipFilter.class, "/*", DISPATCHER_TYPE_ALL);
    }

    public WebServer start() throws Exception {
        server.start();
        return this;
    }

    public WebServer registerServices(String path, Class<?>... entryPoints) {
        Validate.notNull(path, "path");
        Validate.notNull(entryPoints, "entryPoints");

        ServletHolder holder = new ServletHolder(ServletContainer.class);

        StringBuilder sb = new StringBuilder();
        for (Class<?> entryPoint : entryPoints) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(entryPoint.getCanonicalName());
        }

        holder.setInitParameter("jersey.config.server.provider.classnames", sb.toString());
        holder.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
        holder.setInitParameter("jersey.config.server.tracing.type", "ALL");
        holder.setInitParameter("jersey.config.server.tracing.threshold", "VERBOSE");

        handler.addServlet(holder, StringUtils.stripEnd(path, "/") + "/*");

        return this;
    }

    public WebServer registerServlet(String path, Servlet servlet) {
        Validate.notNull(path, "path");
        Validate.notNull(servlet, "servlet");

        ServletHolder holder = new ServletHolder(ServletContainer.class);

        holder.setServlet(servlet);

        handler.addServlet(holder, StringUtils.stripEnd(path, "/") + "/*");

        return this;
    }

    public WebServer registerWebSocketEndpoint(Class<?> endpoint) throws DeploymentException, ServletException {
        WebSocketServerContainerInitializer.configureContext(handler)
            .addEndpoint(endpoint);

        return this;
    }
}
