package com.revenuecat.purchases;

import java.util.HashMap;

class Backend {
    final private String apiKey;
    final private Dispatcher dispatcher;
    final private HTTPClient HTTPClient;
    final private PurchaserInfo.Factory purchaserInfoFactory;

    public static abstract class BackendResponseHandler {
        abstract public void onReceivePurchaserInfo(PurchaserInfo info);
        abstract public void onError(Exception e);
    }

    Backend(String apiKey, Dispatcher dispatcher, com.revenuecat.purchases.HTTPClient httpClient, PurchaserInfo.Factory purchaserInfoFactory) {
        this.apiKey = apiKey;
        this.dispatcher = dispatcher;
        this.HTTPClient = httpClient;
        this.purchaserInfoFactory = purchaserInfoFactory;
    }

    public void getSubscriberInfo(final String appUserID, final BackendResponseHandler handler) {
        dispatcher.enqueue(new Dispatcher.AsyncCall() {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return HTTPClient.performRequest("/subscribers/" + appUserID, null, new HashMap<String, String>());
            }

            @Override
            public void onCompletion(HTTPClient.Result result) {
                if (result.responseCode < 300) {
                    handler.onReceivePurchaserInfo(purchaserInfoFactory.build(result.body));
                } else {
                    handler.onError(new Exception("Received status code " + result.responseCode + " from server."));
                }
            }

            @Override
            void onError(Exception exception) {
                handler.onError(exception);
            };
        });
    }
}
