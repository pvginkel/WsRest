package org.webathome.wsrest.server;

import org.apache.commons.lang3.Validate;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;

class EndpointDescription {
    private final Class<?> klass;
    private final String path;
    private final List<MethodDescription> methods;

    public EndpointDescription(Class<?> klass) throws WsRestException {
        Validate.notNull(klass, "klass");

        this.klass = klass;

        String path = null;
        List<MethodDescription> methods = new ArrayList<>();
        List<String> defaultConsumes = null;
        List<String> defaultProduces = Collections.emptyList();

        for (Annotation annotation : klass.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (annotationType == Path.class) {
                path = ((Path)annotation).value();
            } else if (annotationType == Consumes.class) {
                defaultConsumes = Collections.unmodifiableList(Arrays.asList(((Consumes)annotation).value()));
            } else if (annotationType == Produces.class) {
                defaultProduces = Collections.unmodifiableList(Arrays.asList(((Produces)annotation).value()));
            }
        }

        if (defaultConsumes == null) {
            defaultConsumes = Collections.unmodifiableList(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
        }

        if (path == null) {
            throw new WsRestException("Missing Path annotation on " + klass.getSimpleName());
        }

        this.path = path;

        for (Method method : klass.getMethods()) {
            if (method.getAnnotation(Path.class) != null) {
                methods.add(new MethodDescription(method, defaultProduces, defaultConsumes));
            }
        }

        this.methods = Collections.unmodifiableList(methods);
    }

    public String getPath() {
        return path;
    }

    public Response execute(Request request) throws WsRestException {
        Validate.notNull(request, "request");

        String path = request.getPath();

        assert path.startsWith(this.path);

        path = path.substring(this.path.length());
        String queryStringPart = null;
        int pos = path.indexOf('?');
        if (pos != -1) {
            queryStringPart = path.substring(pos + 1);
            path = path.substring(0, pos);
        }

        // First try methods that do not have a pattern.

        MethodDescription matchedMethod = null;
        Matcher matcher = null;

        for (MethodDescription method : methods) {
            if (
                request.getType() == method.getType() &&
                method.getPathPattern() == null &&
                path.equals(method.getPath())
            ) {
                matchedMethod = method;
                break;
            }
        }

        // Then, ones that do have a pattern.

        if (matchedMethod == null) {
            for (MethodDescription method : methods) {
                if (
                    request.getType() == method.getType() &&
                        method.getPathPattern() != null
                    ) {
                    matcher = method.getPathPattern().matcher(path);
                    if (matcher.find()) {
                        matchedMethod = method;
                        break;
                    }
                }
            }
        }

        if (matchedMethod == null) {
            throw new WsRestException("Not found", ErrorType.NOT_FOUND);
        }

        // Parse the query string.

        Map<String, Object> queryString = null;
        if (queryStringPart != null) {
            queryString = parseUrlEncoded(queryStringPart);
        }

        // Parse the body.

        Map<String, Object> form = null;
        if (matchedMethod.getConsumes() == ParameterEncoding.URL) {
            form = parseUrlEncoded(request.getBody());
        }

        List<ParameterDescription> parameters = matchedMethod.getParameters();
        Object[] args = new Object[parameters.size()];

        // Fill in the path parameters.

        for (int i = 0; i < args.length; i++) {
            ParameterDescription parameter = parameters.get(i);
            Object value = parameter.getDefaultValue();
            switch (parameter.getSource()) {
                case PATH:
                    if (matcher != null) {
                        value = matcher.group("p" + i);
                    }
                    break;

                case QUERY:
                    if (queryString != null) {
                        value = queryString.get(parameter.getName());
                    }
                    break;

                case FORM:
                    if (form != null) {
                        value = form.get(parameter.getName());
                    } else {
                        value = request.getBody();
                    }
                    break;
            }

            args[i] = parameter.getParser().decode(value);
        }

        // Call the method.

        Object result;

        try {
            result = matchedMethod.getMethod().invoke(
                klass.newInstance(),
                args
            );
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new WsRestException("Invoke method failed", e);
        }

        Object encodedResult = matchedMethod.getReturnParameter().getParser().encode(result);
        String stringResult;

        if (encodedResult instanceof String[]) {
            String[] array = (String[])encodedResult;
            switch (array.length) {
                case 0:
                    stringResult = null;
                    break;

                case 1:
                    stringResult = array[0];
                    break;

                default:
                    throw new WsRestException("Cannot serialize return array");
            }
        } else {
            stringResult = (String)encodedResult;
        }

        return new Response(
            ResponseType.OK,
            request.getId(),
            stringResult
        );
    }

    private Map<String, Object> parseUrlEncoded(String input) {
        if (input == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();

        StringTokenizer tokenizer = new StringTokenizer(input, "&");
        while (tokenizer.hasMoreElements()) {
            String token = tokenizer.nextToken();
            int pos = token.indexOf('=');
            String key;
            String value;
            try {
                if (pos == -1) {
                    key = URLDecoder.decode(token, "UTF-8");
                    value = "";
                } else {
                    key = URLDecoder.decode(token.substring(0, pos), "UTF-8");
                    value = URLDecoder.decode(token.substring(pos + 1), "UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                // Does not occur.
                throw new RuntimeException(e);
            }

            Object values = result.get(key);
            if (values == null) {
                result.put(key, value);
            } else if (values instanceof String) {
                result.put(key, new String[]{value, (String)values});
            } else {
                String[] array = (String[])values;
                array = Arrays.copyOf(array, array.length + 1);
                array[array.length - 1] = value;
                result.put(key, array);
            }
        }

        return result;
    }
}
