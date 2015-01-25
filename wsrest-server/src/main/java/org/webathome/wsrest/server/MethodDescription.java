package org.webathome.wsrest.server;

import org.webathome.wsrest.server.annotations.STREAM;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MethodDescription {
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{(.*?)\\}");

    private final Method method;
    private final RequestType type;
    private final String path;
    private final Pattern pathPattern;
    private final List<ParameterDescription> parameters;
    private final ParameterDescription returnParameter;
    private final ParameterEncoding produces;
    private final ParameterEncoding consumes;

    public MethodDescription(Method method, List<String> defaultProduces, List<String> defaultConsumes) throws WsRestException {
        this.method = method;

        RequestType type = RequestType.GET; // Default
        String path = null;
        List<String> produces = defaultProduces;
        List<String> consumes = defaultConsumes;

        for (Annotation annotation : method.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (annotationType == GET.class) {
                type = RequestType.GET;
            } else if (annotationType == POST.class) {
                type = RequestType.POST;
            } else if (annotationType == PUT.class) {
                type = RequestType.PUT;
            } else if (annotationType == DELETE.class) {
                type = RequestType.DELETE;
            } else if (annotationType == STREAM.class) {
                type = RequestType.STREAM;
            } else if (annotationType == Path.class) {
                path = ((Path)annotation).value();
            } else if (annotationType == Produces.class) {
                produces = Collections.unmodifiableList(Arrays.asList(((Produces)annotation).value()));
            } else if (annotationType == Consumes.class) {
                consumes = Collections.unmodifiableList(Arrays.asList(((Consumes)annotation).value()));
            }
        }

        // This is handled upstream.
        assert path != null;

        this.produces = parseParameterEncoding(produces);
        this.consumes = parseParameterEncoding(consumes);
        this.type = type;
        this.path = path;
        this.returnParameter = new ParameterDescription(
            new Annotation[0],
            method.getReturnType(),
            method.getGenericReturnType(),
            "result",
            ParameterSource.RESULT,
            this.produces
        );

        List<ParameterDescription> parameters = new ArrayList<>();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        boolean hadStream = false;

        for (int i = 0; i < parameterTypes.length; i++) {
            ParameterDescription parameter = new ParameterDescription(
                parameterAnnotations[i],
                parameterTypes[i],
                genericParameterTypes != null ? genericParameterTypes[i] : null,
                "arg" + i,
                ParameterSource.QUERY,
                this.consumes
            );

            if (parameter.getSource() == ParameterSource.STREAM) {
                if (hadStream) {
                    throw new WsRestException("Only one Stream parameter can be specified");
                } else {
                    hadStream = true;
                    if (type != RequestType.STREAM) {
                        throw new WsRestException("Stream methods must specify the STREAM annotation");
                    }
                    if (returnParameter.getParser() != null) {
                        throw new WsRestException("Stream methods must have a void return type");
                    }
                }
            }

            parameters.add(parameter);
        }

        // Validate the parameters.

        if (this.consumes != ParameterEncoding.URL) {
            boolean hadOne = false;
            for (ParameterDescription parameter : parameters) {
                if (parameter.getSource() == ParameterSource.FORM) {
                    if (hadOne) {
                        throw new WsRestException(String.format(
                            "Cannot accept multiple %s form parameters for method %s",
                            this.consumes,
                            method.getName()
                        ));
                    } else {
                        hadOne = true;
                    }
                }
            }
        }

        this.parameters = Collections.unmodifiableList(parameters);

        this.pathPattern = buildPathPattern(path);
    }

    private ParameterEncoding parseParameterEncoding(List<String> values) throws WsRestException {
        if (values == null || values.size() == 0) {
            throw new WsRestException("Produces and Consumes annotation must be declared on the endpoint or method");
        }
        if (values.size() > 1) {
            throw new WsRestException("Cannot accept multiple Produces or Consumes mime types");
        }

        switch (values.get(0)) {
            case MediaType.APPLICATION_FORM_URLENCODED:
                return ParameterEncoding.URL;

            case MediaType.APPLICATION_XML:
                return ParameterEncoding.XML;

            case MediaType.APPLICATION_JSON:
                return ParameterEncoding.JSON;

            default:
                if (values.get(0).endsWith("+xml")) {
                    return ParameterEncoding.XML;
                }

                return ParameterEncoding.TEXT;
        }
    }

    private Pattern buildPathPattern(String path) throws WsRestException {
        Matcher matcher = PATH_PARAM_PATTERN.matcher(path);

        StringBuilder sb = new StringBuilder();

        sb.append('^');
        int offset = 0;

        while (matcher.find()) {
            sb.append(Pattern.quote(path.substring(offset, matcher.start())));

            String name = matcher.group(1);

            ParameterDescription param = findParameter(name, ParameterSource.PATH);
            if (param == null) {
                throw new WsRestException("Cannot find path parameter " + name);
            }

            // We name the matched groups, but not by the name of the parameter to prevent conflicts with
            // regular expression syntax.

            int index = parameters.indexOf(param);

            sb.append("(?<p").append(index).append(">.*?)");

            offset = matcher.end();
        }

        // Don't work from a pattern for paths that don't have parameters in them.

        if (offset == 0) {
            return null;
        }

        sb.append(Pattern.quote(path.substring(offset))).append('$');

        return Pattern.compile(sb.toString());
    }

    private ParameterDescription findParameter(String name, ParameterSource source) {
        for (ParameterDescription parameter : parameters) {
            if (parameter.getSource() == source && name.equals(parameter.getName())) {
                return parameter;
            }
        }

        return null;
    }

    public Method getMethod() {
        return method;
    }

    public RequestType getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public Pattern getPathPattern() {
        return pathPattern;
    }

    public List<ParameterDescription> getParameters() {
        return parameters;
    }

    public ParameterDescription getReturnParameter() {
        return returnParameter;
    }

    public ParameterEncoding getProduces() {
        return produces;
    }

    public ParameterEncoding getConsumes() {
        return consumes;
    }
}
