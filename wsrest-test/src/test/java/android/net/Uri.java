package android.net;

import java.net.URI;
import java.net.URISyntaxException;

public class Uri {
    private URI uri;

    public Uri(URI uri) {
        this.uri = uri;
    }

    public static Uri parse(String uri) throws URISyntaxException {
        return new Uri(new URI(uri));
    }

    public String getHost() {
        return uri.getHost();
    }

    public int getPort() {
        return uri.getPort();
    }

    public String getScheme() {
        return uri.getScheme();
    }

    public String getEncodedPath() {
        return uri.getPath();
    }

    public String getEncodedQuery() {
        return uri.getQuery();
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
