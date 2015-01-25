package org.webathome.wsrest.client;

import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;

@SuppressWarnings("UnusedDeclaration")
public class Request {
    private static final Gson GSON = new Gson();
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final Connection connection;
    private final RequestType method;
    private String path;
    private StringBuilder queryString;
    private StringBuilder form;
    private String body;

    Request(Connection connection, RequestType method, String path) {
        this.connection = connection;
        this.method = method;
        this.path = path;
    }

    private String[] encode(Object value) throws WsRestException {
        if (value == null) {
            return EMPTY_STRING_ARRAY;
        }

        Object encoded = ParameterParser.valueParser(value.getClass()).encode(value);
        if (encoded instanceof String) {
            return new String[]{(String)encoded};
        }
        if (encoded == null) {
            return EMPTY_STRING_ARRAY;
        }

        return (String[])encoded;
    }

    public Request addPathParam(String name, Object value) throws WsRestException {
        if (name == null) {
            throw new IllegalArgumentException("name");
        }

        int pos = path.indexOf("{" + name + "}");
        if (pos == -1) {
            throw new WsRestException(String.format("Path does not contain a variable named %s", name));
        }

        StringBuilder sb = new StringBuilder();
        if (pos > 0) {
            sb.append(path, 0, pos);
        }

        String[] encoded = encode(value);

        if (encoded != null) {
            for (int i = 0; i < encoded.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(encoded[i]);
            }
        }

        pos += name.length() + 2;
        if (pos < path.length() - 1) {
            sb.append(path, pos, path.length());
        }

        path = sb.toString();

        return this;
    }

    private void appendUrl(StringBuilder sb, String name, Object value) throws WsRestException {
        String[] encoded = encode(value);

        for (String item : encoded) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            try {
                sb.append(URLEncoder.encode(name, "UTF-8")).append('=');
                if (item != null) {
                    sb.append(URLEncoder.encode(item, "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                // Does not occur.
                throw new RuntimeException(e);
            }
        }
    }

    public Request addFormParam(String name, Object value) throws WsRestException {
        if (name == null) {
            throw new IllegalArgumentException("name");
        }

        if (body != null) {
            throw new WsRestException("Cannot set both a JSON body and form parameters");
        }

        if (form == null) {
            form = new StringBuilder();
        }

        appendUrl(form, name, value);

        return this;
    }

    public Request addQueryParam(String name, Object value) throws WsRestException {
        if (name == null) {
            throw new IllegalArgumentException("name");
        }

        if (queryString == null) {
            queryString = new StringBuilder();
        }

        appendUrl(queryString, name, value);

        return this;
    }

    public Request setJsonBody(Object value) throws WsRestException {
        if (form != null) {
            throw new WsRestException("Cannot set both a JSON body and form parameters");
        }

        if (value != null) {
            body = GSON.toJson(value);
        } else {
            body = "";
        }

        return this;
    }

    public void execute() throws WsRestException {
        execute((Runnable)null);
    }

    public void execute(final Runnable runnable) throws WsRestException {
        getText(new Callback<String>() {
            @Override
            public void call(String value, Throwable e) {
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }

    public String getText() throws WsRestException {
        final Response<String> response = new Response<>();

        getText(response);

        return response.getValue();
    }

    public void getText(Callback<String> callback) throws WsRestException {
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        if (method == RequestType.STREAM) {
            throw new WsRestException("Invalid operation for STREAM request type");
        }

        execute(new PendingTextRequest(callback));
    }

    @SuppressWarnings("unchecked")
    public <T> T getResponse(Class<? extends T> type) throws WsRestException {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }

        return (T)ParameterParser.valueParser(type).decode(getText());
    }

    public <T> void getResponse(final Class<? extends T> type, final Callback<T> callback) throws WsRestException {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        getText(new Callback<String>() {
            @SuppressWarnings("unchecked")
            @Override
            public void call(String value, Throwable e) {
                T result = null;
                if (value != null) {
                    try {
                        result = (T)ParameterParser.valueParser(type).decode(value);
                    } catch (WsRestException e1) {
                        e = e1;
                    }
                }

                callback.call(result, e);
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T getJson(Class<?> type) throws WsRestException {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }

        return (T)GSON.fromJson(getText(), type);
    }

    public <T> T getJson(Type type) throws WsRestException {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }

        return GSON.fromJson(getText(), type);
    }

    @SuppressWarnings("unchecked")
    public <T> void getJson(final Class<?> type, final Callback<T> callback) throws WsRestException {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        getText(new Callback<String>() {
            @Override
            public void call(String value, Throwable e) {
                T result = null;
                if (value != null) {
                    result = (T)GSON.fromJson(value, type);
                }

                callback.call(result, e);
            }
        });
    }

    public <T> void getJson(final Type type, final Callback<T> callback) throws WsRestException {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback");
        }

        getText(new Callback<String>() {
            @SuppressWarnings("unchecked")
            @Override
            public void call(String value, Throwable e) {
                T result = null;
                if (value != null) {
                    result = GSON.fromJson(value, type);
                }

                callback.call(result, e);
            }
        });
    }

    public Stream getStream() throws WsRestException {
        final Response<Stream> response = new Response<>();

        getStream(response);

        return response.getValue();
    }

    public void getStream(Callback<Stream> callback) throws WsRestException {
        if (method != RequestType.STREAM) {
            throw new WsRestException("Request type must be STREAM");
        }

        execute(new PendingStreamRequest(callback, connection));
    }

    private void execute(PendingRequest request) throws WsRestException {
        String path = this.path;
        if (queryString != null) {
            path += "?" + queryString.toString();
        }
        String body = form != null ? form.toString() : this.body;

        connection.execute(method, path, body, request);
    }

    private static class Response<T> implements Callback<T> {
        final ManualResetEvent event = new ManualResetEvent(false);
        T value;
        Throwable error;

        @Override
        public void call(T value, Throwable e) {
            this.value = value;
            this.error = e;
            this.event.set();
        }

        public T getValue() throws WsRestException {
            try {
                event.waitOne();
            } catch (InterruptedException e) {
                throw new WsRestException("Error while executing request", e);
            }

            if (error != null) {
                throw new WsRestException("Error while executing request", error);
            }

            return value;
        }
    }
}
