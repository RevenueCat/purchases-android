package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.OngoingStubbing;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BackendTest {
    private PurchaserInfo.Factory mockInfoFactory;
    private Entitlement.Factory mockEntitlementFactory;
    private HTTPClient mockClient;
    private Dispatcher dispatcher;
    private Backend backend;
    private String API_KEY = "TEST_API_KEY";
    private String appUserID = "jerry";

    private class SyncDispatcher extends Dispatcher {

        SyncDispatcher() {
            super(null);
        }

        @Override
        public void enqueue(AsyncCall call) {
            call.run();
        }
    }

    @Before
    public void setup() {
        mockInfoFactory = mock(PurchaserInfo.Factory.class);
        mockEntitlementFactory = mock(Entitlement.Factory.class);
        mockClient = mock(HTTPClient.class);
        dispatcher = new SyncDispatcher();

        backend = new Backend(API_KEY, dispatcher, mockClient, mockInfoFactory, mockEntitlementFactory);
    }

    @Test
    public void canBeCreated() {
        assertNotNull(backend);
    }

    private PurchaserInfo receivedPurchaserInfo = null;
    private int receivedCode = -1;
    private String receivedMessage = null;
    private Map<String, Entitlement> receivedEntitlements = null;

    final private Backend.BackendResponseHandler handler = new Backend.BackendResponseHandler() {

        @Override
        public void onReceivePurchaserInfo(PurchaserInfo info) {
            BackendTest.this.receivedPurchaserInfo = info;
        }

        @Override
        public void onError(int code, String message) {
            BackendTest.this.receivedCode = -1;
            BackendTest.this.receivedMessage = message;
        }
    };

    final private Backend.EntitlementsResponseHandler entitlementsHandler = new Backend.EntitlementsResponseHandler() {
        @Override
        public void onReceiveEntitlements(Map<String, Entitlement> entitlements) {
            BackendTest.this.receivedEntitlements = entitlements;
        }

        @Override
        public void onError(int code, String message) {
            BackendTest.this.receivedCode = code;
            BackendTest.this.receivedMessage = message;
        }
    };

    private PurchaserInfo mockResponse(String path, Map<String, Object> body, int responseCode, HTTPClient.HTTPErrorException clientException, String resultBody) throws JSONException, HTTPClient.HTTPErrorException {
        if (resultBody == null) {
            resultBody = "{}";
        }

        PurchaserInfo info = mock(PurchaserInfo.class);

        HTTPClient.Result result = new HTTPClient.Result();
        result.responseCode = responseCode;
        result.body = new JSONObject(resultBody);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + API_KEY);

        when(mockInfoFactory.build(result.body)).thenReturn(info);

        OngoingStubbing<HTTPClient.Result> whenStatement = when(mockClient.performRequest(eq(path),
                eq(body), eq(headers)));

        if (clientException == null) {
            whenStatement.thenReturn(result);
        } else {
            whenStatement.thenThrow(clientException);
        }

        return info;
    }

    private PurchaserInfo postReceipt(int responseCode, Boolean isRestore, HTTPClient.HTTPErrorException clientException, String resultBody) throws HTTPClient.HTTPErrorException, JSONException {

        String fetchToken = "fetch_token";
        String productID = "product_id";

        Map<String, Object> body = new HashMap<>();
        body.put("fetch_token", fetchToken);
        body.put("app_user_id", appUserID);
        body.put("product_id", productID);
        body.put("is_restore", isRestore);

        PurchaserInfo info = mockResponse("/receipts", body, responseCode, clientException, resultBody);


        backend.postReceiptData(fetchToken, appUserID, productID, isRestore, handler);

        return info;
    }

    private PurchaserInfo getPurchaserInfo(int responseCode, HTTPClient.HTTPErrorException clientException, String resultBody) throws HTTPClient.HTTPErrorException, JSONException {
        PurchaserInfo info = mockResponse("/subscribers/" + appUserID, null, responseCode, clientException, resultBody);

        backend.getSubscriberInfo(appUserID, handler);

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
        assertNotNull(receivedMessage);
    }

    @Test
    public void clientErrorCallsErrorHandler() throws HTTPClient.HTTPErrorException, JSONException {
        getPurchaserInfo(200, new HTTPClient.HTTPErrorException(0, ""), null);

        assertNull(receivedPurchaserInfo);
        assertNotNull(receivedMessage);
    }

    @Test
    public void attemptsToParseErrorMessageFromServer() throws HTTPClient.HTTPErrorException, JSONException {
        getPurchaserInfo(404, null, "{'message': 'Dude not found'}");

        assertNotNull(receivedMessage);
        assertTrue(receivedMessage.contains("Dude not found"));
    }

    @Test
    public void handlesMissingMessageInErrorBody() throws HTTPClient.HTTPErrorException, JSONException {
        getPurchaserInfo(404, null, "{'no_message': 'Dude not found'}");
        assertNotNull(receivedMessage);
    }

    @Test
    public void postReceiptCallsProperURL() throws HTTPClient.HTTPErrorException, JSONException {
        PurchaserInfo info = postReceipt(200, false, null, null);

        assertNotNull(receivedPurchaserInfo);
        assertSame(info, receivedPurchaserInfo);
    }

    @Test
    public void postReceiptCallsFailsFor40X() throws HTTPClient.HTTPErrorException, JSONException {
        postReceipt(401, false, null, null);

        assertNull(receivedPurchaserInfo);
        assertNotNull(receivedMessage);
    }

    @Test
    public void canGetEntitlementsWhenEmpty() throws HTTPClient.HTTPErrorException, JSONException {

        mockResponse("/subscribers/" + appUserID + "/products",
                null, 200, null, "{'entitlements': {}}");

        backend.getEntitlements(appUserID, entitlementsHandler);

        assertNotNull(receivedEntitlements);
        assertEquals(0, receivedEntitlements.size());
    }

    @Test
    public void canHandleBadEntitlementsResponse() throws HTTPClient.HTTPErrorException, JSONException {

        mockResponse("/subscribers/" + appUserID + "/products",
                null, 200, null, "{}");

        backend.getEntitlements(appUserID, entitlementsHandler);

        assertNull(receivedEntitlements);
        assertNotNull(receivedMessage);
    }

    @Test
    public void passesEntitlementsFieldToFactory() throws HTTPClient.HTTPErrorException, JSONException {
        mockResponse("/subscribers/" + appUserID + "/products",
                null, 200, null, "{'entitlements': {'pro': {}}}");
        when(mockEntitlementFactory.build((JSONObject) ArgumentMatchers.any())).thenReturn(new HashMap<String, Entitlement>());

        backend.getEntitlements(appUserID, entitlementsHandler);

        verify(mockEntitlementFactory).build((JSONObject) ArgumentMatchers.any());
    }

    class CorrectAttributionBody implements ArgumentMatcher<JSONObject> {
        public boolean matches(JSONObject object) {
            return object.has("network") && object.has("data");
        }
    }

    @Test
    public void canPostBasicAttributionData() throws HTTPClient.HTTPErrorException, JSONException {
        String path = "/subscribers/" + appUserID + "/attribution";

        JSONObject object = new JSONObject();
        object.put("string", "value");

        JSONObject expectedBody = new JSONObject();
        expectedBody.put("network", Purchases.AttributionNetwork.APPSFLYER);
        expectedBody.put("data", object);

        backend.postAttributionData(appUserID, Purchases.AttributionNetwork.APPSFLYER, object);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + API_KEY);

        verify(mockClient, times(1)).performRequest(eq(path), argThat(new CorrectAttributionBody()), eq(headers));
    }

    @Test
    public void doesntPostEmptyAttributionData() throws HTTPClient.HTTPErrorException, JSONException {
        String path = "/subscribers/" + appUserID + "/attribution";

        backend.postAttributionData(appUserID, Purchases.AttributionNetwork.APPSFLYER, new JSONObject());

        verifyZeroInteractions(mockClient);
    }

    @Test
    public void encodesAppUserId() throws JSONException, MalformedURLException, HTTPClient.HTTPErrorException {
        String encodeableUserID = "userid with spaces";

        String encodedUserID = "userid%20with%20spaces";
        String path = "/subscribers/" + encodedUserID + "/attribution";

        JSONObject object = new JSONObject();
        object.put("string", "value");

        backend.postAttributionData(encodeableUserID, Purchases.AttributionNetwork.APPSFLYER, object);

        verify(mockClient, times(1)).performRequest(
                eq(path),
                any(JSONObject.class),
                (Map<String, String>)any()
        );

    }
}
