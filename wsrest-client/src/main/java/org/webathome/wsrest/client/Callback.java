package org.webathome.wsrest.client;

public interface Callback<T> {
    void call(T value, Throwable e);
}
