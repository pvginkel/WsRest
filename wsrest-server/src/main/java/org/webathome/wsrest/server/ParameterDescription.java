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

    public ParameterDescription(Annotation[] annotations, Type type, String defaultName, ParameterSource defaultSource, ParameterEncoding encoding) throws WsRestException {
        Validate.notNull(annotations, "annotations");
        Validate.notNull(type, "type");

        String name = null;
        ParameterSource source = null;
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

        this.defaultValue = defaultValue;

        ParameterParser parser = null;

        if (type == Stream.class) {
            if (name != null || defaultValue != null || source != null) {
                throw new WsRestException("Stream parameters cannot have a name, default value or source");
            }

            source = ParameterSource.STREAM;
        } else {
            if (source == null) {
                source = defaultSource;
            }
            if (name == null) {
                name = defaultName;
            }

            if (source != ParameterSource.RESULT || type != Void.TYPE) {
                switch (encoding) {
                    case TEXT:
                        parser = ParameterParser.textParser();
                        break;

                    case JSON:
                        parser = ParameterParser.jsonParser(type);
                        break;

                    case URL:
                        parser = ParameterParser.valueParser(type);
                        break;

                    case XML:
                        parser = ParameterParser.xmlParser();
                        break;
                }
            }
        }

        this.name = name;
        this.source = source;
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
