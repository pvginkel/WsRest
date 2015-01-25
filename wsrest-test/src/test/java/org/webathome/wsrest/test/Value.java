package org.webathome.wsrest.test;

public class Value<T> {
    private T value;

    public Value() {
    }

    public Value(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
