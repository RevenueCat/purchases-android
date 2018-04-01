package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import java.io.IOError;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private PurchaserInfo getPurchaserInfo(int responseCode, HTTPClient.HTTPErrorException clientException) throws HTTPClient.HTTPErrorException, JSONException {
        String appUserID = "jerry";

        PurchaserInfo info = new PurchaserInfo();

        HTTPClient.Result result = new HTTPClient.Result();
        result.responseCode = responseCode;
        result.body = new JSONObject("{}");

        when(mockInfoFactory.build(result.body)).thenReturn(info);

        OngoingStubbing<HTTPClient.Result> whenStatement = when(mockClient.performRequest(eq("/subscribers/" + appUserID),
                (Map) eq(null), any(Map.class)));

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

        PurchaserInfo info = getPurchaserInfo(200, null);

        assertNotNull(receivedPurchaserInfo);
        assertEquals(info, receivedPurchaserInfo);
    }

    @Test
    public void getSubscriberInfoFailsIfNot20X() throws HTTPClient.HTTPErrorException, JSONException {
        int failureCode = ThreadLocalRandom.current().nextInt(300, 500 + 1);

        getPurchaserInfo(failureCode, null);

        assertNull(receivedPurchaserInfo);
        assertNotNull(receivedException);
    }

    @Test
    public void clientErrorCallsErrorHandler() throws HTTPClient.HTTPErrorException, JSONException {
        getPurchaserInfo(200, new HTTPClient.HTTPErrorException());

        assertNull(receivedPurchaserInfo);
        assertNotNull(receivedException);
    }
}
