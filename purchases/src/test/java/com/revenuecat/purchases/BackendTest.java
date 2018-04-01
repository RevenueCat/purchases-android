package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
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
    @Test
    public void getSubscriberInfoCallsProperURL() throws HTTPClient.HTTPErrorException, JSONException {
        String appUserID = "jerry";

        PurchaserInfo info = new PurchaserInfo();

        HTTPClient.Result result = new HTTPClient.Result();
        result.body = new JSONObject("{}");

        when(mockInfoFactory.build(result.body)).thenReturn(info);

        when(mockClient.performRequest(eq("/subscribers/" + appUserID),
                                       (Map) eq(null), any(Map.class))).thenReturn(result);

        backend.getSubscriberInfo(appUserID, new Backend.BackendResponseHandler() {

            @Override
            public void onReceivePurchaserInfo(PurchaserInfo info) {
                BackendTest.this.receivedPurchaserInfo = info;
            }

            @Override
            public void onError(Exception e) {

            }
        });

        assertNotNull(receivedPurchaserInfo);
        assertEquals(info, receivedPurchaserInfo);
    }
}
