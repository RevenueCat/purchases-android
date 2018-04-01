package com.revenuecat.purchases;

import java.util.HashMap;

class Backend {
    final private String apiKey;
    final private Dispatcher dispatcher;
    final private HTTPClient HTTPClient;

    public static abstract class BackendResponseHandler {
        abstract public void onReceivePurchaserInfo(PurchaserInfo info);
        abstract public void onError(Exception e);
    }

    Backend(String apiKey, Dispatcher dispatcher, com.revenuecat.purchases.HTTPClient httpClient) {
        this.apiKey = apiKey;
        this.dispatcher = dispatcher;
        this.HTTPClient = httpClient;
    }

    public void getSubscriberInfo(final String appUserID, final BackendResponseHandler handler) {
        dispatcher.enqueue(new Dispatcher.AsyncCall() {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return HTTPClient.performRequest("/subscribers/" + appUserID, null, new HashMap<String, String>());
            }

            @Override
            public void onCompletion(HTTPClient.Result result) {
                PurchaserInfo info = new PurchaserInfo();
                handler.onReceivePurchaserInfo(info);
            }
        });
    }
}
