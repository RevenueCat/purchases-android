package com.revenuecat.purchases;

import android.util.Log;

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static junit.framework.TestCase.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class HTTPClientTest {
    private static MockWebServer server;
    private static URL baseURL;

    @BeforeClass
    public static void setup() throws IOException {
        server = new MockWebServer();
        baseURL = server.url("/v1").url();
    }

    @Test
    public void canBeCreated() {
        new HTTPClient(baseURL);
    }

    @Test
    public void canPerformASimpleGet() throws HTTPClient.HTTPErrorException, InterruptedException {
        MockResponse response = new MockResponse().setBody("{}");
        server.enqueue(response);

        HTTPClient client = new HTTPClient(baseURL);
        client.performRequest("/resource", null, null);

        RecordedRequest request = server.takeRequest();
        assertEquals(request.getMethod(), "GET");
        assertEquals(request.getPath(), "/resource");
    }

    // Errors

    @Test(expected = HTTPClient.HTTPErrorException.class)
    public void reWrapsBadJSONError() throws HTTPClient.HTTPErrorException, InterruptedException {
        MockResponse response = new MockResponse().setBody("not uh jason");
        server.enqueue(response);

        HTTPClient client = new HTTPClient(baseURL);
        client.performRequest("/resource", null, null);
    }

    @Test(expected = RuntimeException.class)
    public void badURLsThrowRuntimeException() throws HTTPClient.HTTPErrorException {
        HTTPClient client = new HTTPClient(baseURL);
        client.performRequest(" https://", null, null);
    }

    // Headers
    @Test
    public void addsHeadersToRequest() throws HTTPClient.HTTPErrorException, InterruptedException {
        MockResponse response = new MockResponse().setBody("{}");
        server.enqueue(response);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authentication", "Bearer todd");

        HTTPClient client = new HTTPClient(baseURL);
        client.performRequest("/resource", null, headers);

        RecordedRequest request = server.takeRequest();
        assertEquals(request.getHeader("Authentication"), "Bearer todd");
    }

    @Test
    public void addsDefaultHeadersToRequest() throws HTTPClient.HTTPErrorException, InterruptedException {
        MockResponse response = new MockResponse().setBody("{}");
        server.enqueue(response);

        HTTPClient client = new HTTPClient(baseURL);
        client.performRequest("/resource", null, null);

        RecordedRequest request = server.takeRequest();

        assertEquals(request.getHeader("Content-Type"), "application/json");
        assertEquals(request.getHeader("X-Platform"), "android");
        assertEquals(request.getHeader("X-Platform-Version"), Integer.toString(android.os.Build.VERSION.SDK_INT));
        assertEquals(request.getHeader("X-Version"), "0.1.0-SNAPSHOT");
    }

    @AfterClass
    public static void teardown() throws IOException {
        server.shutdown();
    }
}
