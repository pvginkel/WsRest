package org.webathome.wsrest.server;

import org.apache.commons.lang3.Validate;

import javax.ws.rs.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

class ParameterDescription {
    private final String name;
    private final ParameterSource source;
    private final String defaultValue;
    private final ParameterParser parser;

    public ParameterDescription(Annotation[] annotations, Class<?> type, Type genericType, String defaultName, ParameterSource defaultSource, ParameterEncoding encoding) throws WsRestException {
        Validate.notNull(annotations, "annotations");
        Validate.notNull(type, "type");

        String name = defaultName;
        ParameterSource source = defaultSource;
        String defaultValue = null;

        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();

            if (annotationType == QueryParam.class) {
                source = ParameterSource.QUERY;
                name = ((QueryParam)annotation).value();
            } else if (annotationType == FormParam.class) {
                source = ParameterSource.FORM;
                name = ((FormParam)annotation).value();
            } else if (annotationType == PathParam.class) {
                source = ParameterSource.PATH;
                name = ((PathParam)annotation).value();
            } else if (annotationType == DefaultValue.class) {
                defaultValue = ((DefaultValue)annotation).value();
            }
        }

        this.name = name;
        this.source = source;
        this.defaultValue = defaultValue;

        ParameterParser parser = null;

        switch (encoding) {
            case TEXT:
                parser = ParameterParser.textParser();
                break;

            case JSON:
                parser = ParameterParser.jsonParser(type, genericType);
                break;

            case URL:
                parser = ParameterParser.valueParser(type, genericType);
                break;

            case XML:
                parser = ParameterParser.xmlParser();
                break;
        }

        this.parser = parser;
    }

    public String getName() {
        return name;
    }

    public ParameterParser getParser() {
        return parser;
    }

    public ParameterSource getSource() {
        return source;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
