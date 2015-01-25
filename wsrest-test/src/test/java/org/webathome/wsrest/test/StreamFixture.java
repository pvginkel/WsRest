package org.webathome.wsrest.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.webathome.wsrest.client.RequestType;
import org.webathome.wsrest.client.Stream;
import org.webathome.wsrest.client.WsRestException;
import org.webathome.wsrest.test.support.AutoResetEvent;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class StreamFixture extends FixtureBase {
    @Test
    public void openStream() throws WsRestException, InterruptedException {
        Stream stream = openConnection()
            .newRequest("/stream/open-stream", RequestType.STREAM)
            .addQueryParam("count", 10)
            .addQueryParam("interval", 10)
            .getStream();

        final AutoResetEvent event = new AutoResetEvent(false);
        final Value<Integer> count = new Value<>(10);
        final Value<Boolean> closed = new Value<>(false);
        final Object syncRoot = new Object();

        stream.setCallback(new Stream.Callback() {
            @Override
            public void onMessage(String message) {
                synchronized (syncRoot) {
                    count.set(count.get() - 1);
                    event.set();
                }
            }

            @Override
            public void onClosed() {
                synchronized (syncRoot) {
                    closed.set(true);
                    event.set();
                }
            }

            @Override
            public void onError(Throwable e) {
                synchronized (syncRoot) {
                    closed.set(true);
                    event.set();
                }
            }
        });

        while (true) {
            synchronized (syncRoot) {
                assertFalse(closed.get());

                if (count.get() == 0) {
                    break;
                }
            }

            event.waitOne();
        }
    }

    @Test
    public void echoEarlyClose() throws WsRestException, InterruptedException {
        Stream stream = openConnection(true)
            .newRequest("/stream/echo", RequestType.STREAM)
            .getStream();

        final AutoResetEvent closedEvent = new AutoResetEvent(false);

        StreamApi.getInstance().setClosed(new Runnable() {
            @Override
            public void run() {
                closedEvent.set();
            }
        });

        stream.setCallback(new Stream.Callback() {
            @Override
            public void onMessage(String message) {

            }

            @Override
            public void onClosed() {

            }

            @Override
            public void onError(Throwable e) {

            }
        });

        for (int i = 0; i < 100; i++) {
            stream.sendText(String.valueOf(i));
        }

        stream.close();

        closedEvent.waitOne();
    }

    @Test
    public void echoFullWait() throws WsRestException, InterruptedException {
        Stream stream = openConnection()
            .newRequest("/stream/echo", RequestType.STREAM)
            .getStream();

        final AutoResetEvent closedEvent = new AutoResetEvent(false);
        final AutoResetEvent messageEvent = new AutoResetEvent(false);

        StreamApi.getInstance().setClosed(new Runnable() {
            @Override
            public void run() {
                closedEvent.set();
            }
        });

        final Set<Integer> ids = new HashSet<>();
        final Object syncRoot = new Object();

        stream.setCallback(new Stream.Callback() {
            @Override
            public void onMessage(String message) {
                int id = Integer.parseInt(message);

                synchronized (syncRoot) {
                    assertTrue(ids.add(id));
                }

                messageEvent.set();
            }

            @Override
            public void onClosed() {

            }

            @Override
            public void onError(Throwable e) {

            }
        });

        for (int i = 0; i < 100; i++) {
            stream.sendText(String.valueOf(i));
        }

        while (true) {
            synchronized (syncRoot) {
                if (ids.size() == 100) {
                    break;
                }
            }

            messageEvent.waitOne();
        }

        stream.close();

        closedEvent.waitOne();

        assertEquals(100, ids.size());
    }

    @Test
    public void echoPingPong() throws WsRestException, InterruptedException {
        Stream stream = openConnection()
            .newRequest("/stream/echo", RequestType.STREAM)
            .getStream();

        final AutoResetEvent closedEvent = new AutoResetEvent(false);
        final AutoResetEvent messageEvent = new AutoResetEvent(false);

        StreamApi.getInstance().setClosed(new Runnable() {
            @Override
            public void run() {
                closedEvent.set();
            }
        });

        final Value<Integer> nextId = new Value<>(0);
        final Object syncRoot = new Object();

        stream.setCallback(new Stream.Callback() {
            @Override
            public void onMessage(String message) {
                int id = Integer.parseInt(message);

                synchronized (syncRoot) {
                    assertEquals(id, (int)nextId.get());
                    nextId.set(nextId.get() + 1);
                }

                messageEvent.set();
            }

            @Override
            public void onClosed() {

            }

            @Override
            public void onError(Throwable e) {

            }
        });

        for (int i = 0; i < 100; i++) {
            stream.sendText(String.valueOf(i));

            messageEvent.waitOne();
        }

        stream.close();

        closedEvent.waitOne();

        assertEquals(100, (int)nextId.get());
    }
}
