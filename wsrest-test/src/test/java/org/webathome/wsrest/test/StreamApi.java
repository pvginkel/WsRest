package org.webathome.wsrest.test;

import org.webathome.wsrest.server.Stream;
import org.webathome.wsrest.server.WsRestException;
import org.webathome.wsrest.server.annotations.STREAM;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@SuppressWarnings("UnusedDeclaration")
@Path("/stream")
@Produces(MediaType.APPLICATION_FORM_URLENCODED)
public class StreamApi {
    private static volatile StreamApi instance;

    public static StreamApi getInstance() {
        return instance;
    }

    private volatile Runnable closed;

    public StreamApi() {
        instance = this;
    }

    public void setClosed(Runnable closed) {
        this.closed = closed;
    }

    @STREAM
    @Path("/open-stream")
    public void openStream(
        @QueryParam("count") final int count,
        @QueryParam("interval") final int interval,
        final Stream stream
    ) {
        stream.setCallback(new Stream.Callback() {
            @Override
            public void onMessage(String message) {
                System.out.println("Received " + message);
            }

            @Override
            public void onClosed() {
                System.out.println("Stream closed");
            }

            @Override
            public void onError(Throwable e) {
                System.out.println("Stream error " + e.getMessage());
            }
        });

        new Thread() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < count; i++) {
                        Thread.sleep(interval);

                        stream.sendText("Ping " + i);
                    }

                    stream.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    @STREAM
    @Path("/close-immediately")
    public void closeImmediately(Stream stream) throws WsRestException {
        stream.close();
    }

    @STREAM
    @Path("/echo")
    public void echo(final Stream stream) {
        stream.setCallback(new Stream.Callback() {
            @Override
            public void onMessage(String message) {
                try {
                    stream.sendText(message);
                } catch (WsRestException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onClosed() {
                Runnable closed = StreamApi.this.closed;
                if (closed != null) {
                    closed.run();
                }
            }

            @Override
            public void onError(Throwable e) {
                Runnable closed = StreamApi.this.closed;
                if (closed != null) {
                    closed.run();
                }
            }
        });
    }
}
