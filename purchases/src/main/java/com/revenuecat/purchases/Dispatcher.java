package com.revenuecat.purchases;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


class Dispatcher {
    abstract static class AsyncCall implements Runnable {
        abstract public HTTPClient.Result call() throws HTTPClient.HTTPErrorException;
        void onError(Exception exception) {};
        void onCompletion(HTTPClient.Result result) {};

        @Override
        public void run() {
            try {
                HTTPClient.Result result = call();
                onCompletion(result);
            } catch (HTTPClient.HTTPErrorException e) {
                onError(e);
            }
        }
    }

    private ExecutorService executorService;

    Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public Dispatcher() {}

    public void enqueue(AsyncCall call) {
        this.executorService.submit(call);
    }
}
