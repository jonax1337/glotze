package local.tvhome;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Minimal blocking HTTPS GET for small JSON documents and arrays; call it off the main thread. */
final class HttpJson {
    private static final int TIMEOUT_MS = 8000;
    private static final int BUFFER_SIZE = 8192;
    private static final String USER_AGENT = "tvhome-launcher";

    private HttpJson() {
    }

    static JSONObject get(String url) throws IOException, JSONException {
        return new JSONObject(download(url));
    }

    static JSONArray getArray(String url) throws IOException, JSONException {
        return new JSONArray(download(url));
    }

    private static String download(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(TIMEOUT_MS);
        connection.setReadTimeout(TIMEOUT_MS);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try (InputStream body = connection.getInputStream()) {
            return readUtf8(body);
        } finally {
            connection.disconnect();
        }
    }

    private static String readUtf8(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
