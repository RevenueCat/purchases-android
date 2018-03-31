package com.revenuecat.purchases;

class Backend {
    final private String apiKey;
    final private Dispatcher dispatcher;
    final private HTTPClient HTTPClient;

    Backend(String apiKey, Dispatcher dispatcher, com.revenuecat.purchases.HTTPClient httpClient) {
        this.apiKey = apiKey;
        this.dispatcher = dispatcher;
        this.HTTPClient = httpClient;
    }
}
