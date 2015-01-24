package org.webathome.wsrest.server;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

class Request {
    private final RequestType type;
    private final String path;
    private final long id;
    private final String body;

    public static Request parse(String text) throws WsRestException {
        Validate.notNull(text, "text");

        RequestType type;
        String path;
        long id;
        String body = null;

        int pos = text.indexOf('\n');

        if (pos != -1) {
            body = text.substring(pos + 1);
            text = text.substring(0, pos);
        }

        String[] parts = StringUtils.split(text, ' ');
        if (parts.length != 3) {
            throw new WsRestException("Invalid request", ErrorType.INVALID_REQUEST);
        }

        switch (parts[0]) {
            case "GET":
                type = RequestType.GET;
                break;

            case "POST":
                type = RequestType.POST;
                break;

            case "PUT":
                type = RequestType.PUT;
                break;

            case "DELETE":
                type = RequestType.DELETE;
                break;

            default:
                throw new WsRestException("Invalid method " + parts[0], ErrorType.INVALID_REQUEST);
        }

        path = parts[1];

        try {
            id = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new WsRestException("Invalid request", ErrorType.INVALID_REQUEST);
        }

        return new Request(type, path, id, body);
    }

    private Request(RequestType type, String path, long id, String body) {
        this.type = type;
        this.path = path;
        this.id = id;
        this.body = body;
    }

    public RequestType getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public long getId() {
        return id;
    }

    public String getBody() {
        return body;
    }
}
