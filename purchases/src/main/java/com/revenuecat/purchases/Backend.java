package com.revenuecat.purchases;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

class Backend {
    final private String apiKey;
    final private Dispatcher dispatcher;
    final private HTTPClient httpClient;
    final private PurchaserInfo.Factory purchaserInfoFactory;
    final private Map<String, String> authenticationHeaders;

    public static abstract class BackendResponseHandler {
        abstract public void onReceivePurchaserInfo(PurchaserInfo info);
        abstract public void onError(Exception e);
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
                    handler.onError(e);
                }
            } else {
                Exception e = null;
                try {
                    String message = result.body.getString("message");
                    e = new Exception("Server error " + result.responseCode + ": " + message);
                } catch (JSONException jsonException) {
                    e = new Exception("Unexpected server error " + result.responseCode);
                }

                handler.onError(e);
            }
        }

        @Override
        void onError(Exception exception) {
            handler.onError(exception);
        };
    }

    Backend(String apiKey, Dispatcher dispatcher, com.revenuecat.purchases.HTTPClient httpClient, PurchaserInfo.Factory purchaserInfoFactory) {
        this.apiKey = apiKey;
        this.dispatcher = dispatcher;
        this.httpClient = httpClient;
        this.purchaserInfoFactory = purchaserInfoFactory;

        this.authenticationHeaders = new HashMap<>();
        this.authenticationHeaders.put("Authentication", "Bearer " + this.apiKey);
    }

    public void getSubscriberInfo(final String appUserID, final BackendResponseHandler handler) {
        dispatcher.enqueue(new PurchaserInfoReceivingCall(handler) {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return httpClient.performRequest("/subscribers/" + appUserID, null, authenticationHeaders);
            }
        });
    }

    public void postReceiptData(final String purchaseToken, final String appUserID, final String productID, final BackendResponseHandler handler) {
        final Map<String, String> body = new HashMap<>();

        body.put("fetch_token", purchaseToken);
        body.put("product_id", productID);
        body.put("app_user_id", appUserID);

        dispatcher.enqueue(new PurchaserInfoReceivingCall(handler) {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return httpClient.performRequest("/receipts", body, authenticationHeaders);
            }
        });
    }
}
