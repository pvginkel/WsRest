package org.webathome.wsrest.server;

import org.apache.commons.lang3.Validate;

class Response {
    private final ResponseType type;
    private final long id;
    private final String body;

    public Response(ResponseType type, long id, String body) {
        Validate.notNull(type, "type");

        this.type = type;
        this.id = id;
        this.body = body;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append(type.name())
            .append(' ')
            .append(id);

        if (body != null) {
            sb.append('\n').append(body);
        }

        return sb.toString();
    }
}
