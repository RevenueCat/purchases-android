package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

class HTTPClient {
    class Result {
        int responseCode;
        JSONObject body;
    }

    class HTTPErrorException extends Exception {}

    private final URL baseURL;

    HTTPClient() {
        try {
            this.baseURL = new URL("https://api.revenuecat.com/v1");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    HTTPClient(URL baseURL) {
        this.baseURL = baseURL;
    }

    /** Buffers the given {@code InputStream}. */
    private static BufferedReader buffer(InputStream is) {
        return new BufferedReader(new InputStreamReader(is));
    }

    /** Buffers the given {@code OutputStream}. */
    private static BufferedWriter buffer(OutputStream os) {
        return new BufferedWriter(new OutputStreamWriter(os));
    }

    /** Reads the given {@code InputStream} into a String. */
    private static String readFully(InputStream is) throws IOException {
        return readFully(buffer(is));
    }

    /** Reads the given {@code BufferedReader} into a String. */
    private static String readFully(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static InputStream getInputStream(HttpURLConnection connection) {
        try {
            return connection.getInputStream();
        } catch (IOException e) {
            return connection.getErrorStream();
        }
    }

    private static void writeFully(BufferedWriter writer, String body) throws IOException {
        writer.write(body);
    }

    public Result performRequest(final String path,
                                 final Map body,
                                 final Map<String, String> headers)
            throws HTTPErrorException {
        URL fullURL = null;
        try {
            fullURL = new URL(baseURL, path);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection)fullURL.openConnection();

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("X-Platform", "android");
            connection.addRequestProperty("X-Platform-Version", Integer.toString(android.os.Build.VERSION.SDK_INT));
            connection.addRequestProperty("X-Version", "0.1.0-SNAPSHOT"); // FIXME

            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                OutputStream os = connection.getOutputStream();
                writeFully(buffer(os), new JSONObject(body).toString());
            }
        } catch (IOException e) {
            throw new HTTPErrorException();
        }

        InputStream in = getInputStream(connection);
        HTTPClient.Result result = new HTTPClient.Result();

        String payload = null;
        try {
            result.responseCode = connection.getResponseCode();
            payload = readFully(in);
        } catch (IOException e) {
            throw new HTTPErrorException();
        } finally {
            connection.disconnect();
        }

        try {
            result.body = new JSONObject(payload);
        } catch (JSONException e) {
            throw new HTTPErrorException();
        }

        return result;
    }
}
