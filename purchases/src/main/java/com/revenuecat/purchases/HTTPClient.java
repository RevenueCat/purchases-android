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

import static com.revenuecat.purchases.Purchases.getFrameworkVersion;

class HTTPClient {
    static class Result {
        int responseCode;
        JSONObject body;
    }

    static class HTTPErrorException extends Exception {
        HTTPErrorException(int httpCode, String message) {
            super("[" + httpCode + "]: " + message);
        }
    }

    private final URL baseURL;

    HTTPClient() {
        try {
            this.baseURL = new URL("http://10.0.2.2:5000/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    HTTPClient(URL baseURL) {
        this.baseURL = baseURL;
    }

    private static BufferedReader buffer(InputStream is) {
        return new BufferedReader(new InputStreamReader(is));
    }

    private static BufferedWriter buffer(OutputStream os) {
        return new BufferedWriter(new OutputStreamWriter(os));
    }

    private static String readFully(InputStream is) throws IOException {
        return readFully(buffer(is));
    }

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

    /** Performs a synchronous web request to the RevenueCat API
     * @param path The resource being requested
     * @param body The body of the request, for GET must be null
     * @param headers Map of headers, basic headers are added automatically
     * @return Result containing the HTTP response code and the parsed JSON body
     * @throws HTTPErrorException Thrown for any unexpected errors, not thrown for returned HTTP error codes
     */
    public Result performRequest(final String path,
                                 final Map body,
                                 final Map<String, String> headers)
            throws HTTPErrorException {
        URL fullURL = null;
        try {
            fullURL = new URL(baseURL, "/v1" + path);
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
            connection.addRequestProperty("X-Version", getFrameworkVersion()); // FIXME

            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                OutputStream os = connection.getOutputStream();
                writeFully(buffer(os), new JSONObject(body).toString());
            }
        } catch (IOException e) {
            throw new HTTPErrorException(-1, "Error establishing connection " + e.getMessage());
        }

        InputStream in = getInputStream(connection);
        HTTPClient.Result result = new HTTPClient.Result();

        String payload = null;
        try {
            result.responseCode = connection.getResponseCode();
            payload = readFully(in);
        } catch (IOException e) {
            throw new HTTPErrorException(result.responseCode, "Error reading response: " + e.getMessage());
        } finally {
            connection.disconnect();
        }

        try {
            result.body = new JSONObject(payload);
        } catch (JSONException e) {
            throw new HTTPErrorException(result.responseCode, "Error parsing JSON body: " + payload);
        }

        return result;
    }
}
