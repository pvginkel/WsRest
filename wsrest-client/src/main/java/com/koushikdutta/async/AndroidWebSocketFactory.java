package com.koushikdutta.async;

import org.webathome.wsrest.client.WebSocket;
import org.webathome.wsrest.client.WebSocketCallback;
import org.webathome.wsrest.client.WebSocketFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AndroidWebSocketFactory implements WebSocketFactory {
    private static final int DEFAULT_THREAD_COUNT = 1;
    private ThreadPoolExecutor threadPool;

    public AndroidWebSocketFactory() {
        this(new ThreadPoolExecutor(
            0,
            DEFAULT_THREAD_COUNT,
            10,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>()
        ));
    }

    public AndroidWebSocketFactory(ThreadPoolExecutor threadPool) {
        if (threadPool == null) {
            throw new IllegalArgumentException("threadPool");
        }

        this.threadPool = threadPool;
    }

    @Override
    public WebSocket newInstance(String url, WebSocketCallback callback) throws Exception {
        return new WebSocketImpl(url, callback, threadPool);
    }
}
