package com.revenuecat.purchases;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

class Backend {
    final private String apiKey;
    final private Dispatcher dispatcher;
    final private HTTPClient httpClient;
    final private PurchaserInfo.Factory purchaserInfoFactory;
    final private Entitlement.Factory entitlementFactory;
    final private Map<String, String> authenticationHeaders;

    public static abstract class BackendResponseHandler {
        abstract public void onReceivePurchaserInfo(PurchaserInfo info);
        abstract public void onError(int code, String message);
    }

    public static abstract class EntitlementsResponseHandler {
        abstract public void onReceiveEntitlements(Map<String, Entitlement> entitlements);
        abstract public void onError(int code, String message);
    }

    private abstract class PurchaserInfoReceivingCall extends Dispatcher.AsyncCall {
        final private BackendResponseHandler handler;
        PurchaserInfoReceivingCall(BackendResponseHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onCompletion(HTTPClient.Result result) {
            if (result.responseCode < 300) {
                try {
                    handler.onReceivePurchaserInfo(purchaserInfoFactory.build(result.body));
                } catch (JSONException e) {
                    handler.onError(result.responseCode, e.getMessage());
                }
            } else {
                String errorMessage = null;
                try {
                    String message = result.body.getString("message");
                    errorMessage = "Server error: " + message;
                } catch (JSONException jsonException) {
                    errorMessage = "Unexpected server error " + result.responseCode;
                }

                handler.onError(result.responseCode, errorMessage);
            }
        }

        @Override
        void onError(int code, String message) {
            handler.onError(code, message);
        };
    }

    Backend(String apiKey, Dispatcher dispatcher, HTTPClient httpClient, PurchaserInfo.Factory purchaserInfoFactory, Entitlement.Factory entitlementFactory) {
        this.apiKey = apiKey;
        this.dispatcher = dispatcher;
        this.httpClient = httpClient;
        this.purchaserInfoFactory = purchaserInfoFactory;
        this.entitlementFactory = entitlementFactory;

        this.authenticationHeaders = new HashMap<>();
        this.authenticationHeaders.put("Authorization", "Bearer " + this.apiKey);
    }

    public void getSubscriberInfo(final String appUserID, final BackendResponseHandler handler) {
        dispatcher.enqueue(new PurchaserInfoReceivingCall(handler) {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return httpClient.performRequest("/subscribers/" + appUserID, (Map)null, authenticationHeaders);
            }
        });
    }

    public void postReceiptData(final String purchaseToken, final String appUserID, final String productID, final Boolean isRestore, final BackendResponseHandler handler) {
        final Map<String, Object> body = new HashMap<>();

        body.put("fetch_token", purchaseToken);
        body.put("product_id", productID);
        body.put("app_user_id", appUserID);
        body.put("is_restore", isRestore);

        dispatcher.enqueue(new PurchaserInfoReceivingCall(handler) {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return httpClient.performRequest("/receipts", body, authenticationHeaders);
            }
        });
    }

    void getEntitlements(final String appUserID, final EntitlementsResponseHandler handler) {
        dispatcher.enqueue(new Dispatcher.AsyncCall() {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return httpClient.performRequest("/subscribers/" + appUserID + "/products",(Map)null, authenticationHeaders);
            }

            void onError(int code, String message) {
                handler.onError(code, message);
            }

            void onCompletion(HTTPClient.Result result) {
                if (result.responseCode < 300) {
                    try {
                        JSONObject entitlementsResponse = result.body.getJSONObject("entitlements");
                        Map<String, Entitlement> entitlementMap = entitlementFactory.build(entitlementsResponse);
                        handler.onReceiveEntitlements(entitlementMap);
                    } catch (JSONException e) {
                        handler.onError(result.responseCode, "Error parsing products JSON " + e.getLocalizedMessage());
                    }
                } else {
                    handler.onError(result.responseCode, "Backend error");
                }
            }
        });
    }

    void postAttributionData(final String appUserID, @Purchases.AttributionNetwork int network, JSONObject data) {
        if (data.length() == 0) return;

        final JSONObject body = new JSONObject();
        try {
            body.put("network", network);
            body.put("data", data);
        } catch (JSONException e) {
            return;
        }

        dispatcher.enqueue(new Dispatcher.AsyncCall() {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return httpClient.performRequest("/subscribers/" + appUserID + "/attribution", body, authenticationHeaders);
            }
        });
    }
}
