package org.webathome.wsrest.test.support;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class WebUtil {
    public static String getResponse(String url, String post) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();

        connection.setDoOutput(post != null);
        connection.setDoInput(true);

        if (post != null) {
            connection.setRequestMethod("POST");

            try (OutputStream os = connection.getOutputStream()) {
                IOUtils.write(post, os);
            }
        }

        connection.connect();

        try (InputStream is = connection.getInputStream()) {
            if (isCompressed(connection)) {
                try (InputStream zipIs = new GZIPInputStream(is)) {
                    return IOUtils.toString(zipIs);
                }
            } else {
                return IOUtils.toString(is);
            }
        }
    }

    private static boolean isCompressed(HttpURLConnection connection) {
        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            if ("Content-Encoding".equalsIgnoreCase(entry.getKey())) {
                return "gzip".equalsIgnoreCase(entry.getValue().get(0));
            }
        }

        return false;
    }
}
