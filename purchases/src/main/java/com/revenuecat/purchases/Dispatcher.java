package com.revenuecat.purchases;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;


class Dispatcher {
    abstract static class AsyncCall implements Runnable {
        abstract public HTTPClient.Result call() throws HTTPClient.HTTPErrorException;
        void onError(int code, String message) {}
        void onCompletion(HTTPClient.Result result) {}

        @Override
        public void run() {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            try {
                final HTTPClient.Result result = call();
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onCompletion(result);
                    }
                });
            } catch (final HTTPClient.HTTPErrorException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onError(0, e.getMessage());
                    }
                });
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
    public void close() {
        this.executorService.shutdownNow();
    }
    public boolean isClosed() {
        return this.executorService.isShutdown();
    }
}
