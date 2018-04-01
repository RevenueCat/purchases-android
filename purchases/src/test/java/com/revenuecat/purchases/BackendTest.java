package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.OngoingStubbing;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class BackendTest {
    private PurchaserInfo.Factory mockInfoFactory;
    private HTTPClient mockClient;
    private Dispatcher dispatcher;
    private Backend backend;
    private String API_KEY = "TEST_API_KEY";

    private class SyncDispatcher extends Dispatcher {
        @Override
        public void enqueue(AsyncCall call) {
            call.run();
        }
    }

    @Before
    public void setup() {
        mockInfoFactory = mock(PurchaserInfo.Factory.class);
        mockClient = mock(HTTPClient.class);
        dispatcher = new SyncDispatcher();

        backend = new Backend(API_KEY, dispatcher, mockClient, mockInfoFactory);
    }

    @Test
    public void canBeCreated() {
        assertNotNull(backend);
    }

    private PurchaserInfo receivedPurchaserInfo = null;
    private Exception receivedException = null;

    private PurchaserInfo getPurchaserInfo(int responseCode, HTTPClient.HTTPErrorException clientException, String resultBody) throws HTTPClient.HTTPErrorException, JSONException {
        String appUserID = "jerry";
        if (resultBody == null) {
            resultBody = "{}";
        }

        PurchaserInfo info = new PurchaserInfo();

        HTTPClient.Result result = new HTTPClient.Result();
        result.responseCode = responseCode;
        result.body = new JSONObject(resultBody);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authentication", "Bearer " + API_KEY);

        when(mockInfoFactory.build(result.body)).thenReturn(info);

        OngoingStubbing<HTTPClient.Result> whenStatement = when(mockClient.performRequest(eq("/subscribers/" + appUserID),
                (Map) eq(null), eq(headers)));

        if (clientException == null) {
            whenStatement.thenReturn(result);
        } else {
            whenStatement.thenThrow(clientException);
        }


        backend.getSubscriberInfo(appUserID, new Backend.BackendResponseHandler() {

            @Override
            public void onReceivePurchaserInfo(PurchaserInfo info) {
                BackendTest.this.receivedPurchaserInfo = info;
            }

            @Override
            public void onError(Exception e) {
                BackendTest.this.receivedException = e;
            }
        });

        return info;
    }

    @Test
    public void getSubscriberInfoCallsProperURL() throws HTTPClient.HTTPErrorException, JSONException {

        PurchaserInfo info = getPurchaserInfo(200, null, null);

        assertNotNull(receivedPurchaserInfo);
        assertEquals(info, receivedPurchaserInfo);
    }

    @Test
    public void getSubscriberInfoFailsIfNot20X() throws HTTPClient.HTTPErrorException, JSONException {
        int failureCode = ThreadLocalRandom.current().nextInt(300, 500 + 1);

        getPurchaserInfo(failureCode, null, null);

        assertNull(receivedPurchaserInfo);
        assertNotNull(receivedException);
    }

    @Test
    public void clientErrorCallsErrorHandler() throws HTTPClient.HTTPErrorException, JSONException {
        getPurchaserInfo(200, new HTTPClient.HTTPErrorException(), null);

        assertNull(receivedPurchaserInfo);
        assertNotNull(receivedException);
    }

    @Test
    public void attemptsToParseErrorMessageFromServer() throws HTTPClient.HTTPErrorException, JSONException {
        getPurchaserInfo(404, null, "{'message': 'Dude not found'}");

        assertNotNull(receivedException);
        assertTrue(receivedException.getMessage().contains("Dude not found"));
    }

    @Test
    public void handlesMissingMessageInErrorBody() throws HTTPClient.HTTPErrorException, JSONException {
        getPurchaserInfo(404, null, "{'no_message': 'Dude not found'}");
        assertNotNull(receivedException);
    }
}
