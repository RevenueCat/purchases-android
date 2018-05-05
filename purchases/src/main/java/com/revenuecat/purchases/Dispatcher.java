package com.revenuecat.purchases;

import java.util.concurrent.ExecutorService;


class Dispatcher {
    abstract static class AsyncCall implements Runnable {
        abstract public HTTPClient.Result call() throws HTTPClient.HTTPErrorException;
        void onError(int code, String message) {};
        void onCompletion(HTTPClient.Result result) {};

        @Override
        public void run() {
            try {
                HTTPClient.Result result = call();
                onCompletion(result);
            } catch (HTTPClient.HTTPErrorException e) {
                onError(0, e.getMessage());
            }
        }
    }

    private ExecutorService executorService;

    Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void enqueue(AsyncCall call) {
        this.executorService.submit(call);
    }
}
