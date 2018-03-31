package com.revenuecat.purchases;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DispatcherTest {
    private Dispatcher dispatcher;
    private ExecutorService executorService;

    @Before
    public void setup() {
        executorService = mock(ExecutorService.class);
        dispatcher = new Dispatcher(executorService);
    }

    @Test
    public void canBeCreated() {
        assertNotNull(dispatcher);
    }

    @Test
    public void submitsToExecutor() {
        final HTTPClient.Result result = new HTTPClient.Result();

        when(executorService.submit(any(Dispatcher.AsyncCall.class))).thenReturn(null);

        dispatcher.enqueue(new Dispatcher.AsyncCall() {
            @Override
            public HTTPClient.Result call() {
                return result;
            }
        });

        verify(executorService).submit(any(Dispatcher.AsyncCall.class));
    }

    private Boolean errorCalled = false;
    @Test
    public void asyncCallHandlesFailures() {
        Dispatcher.AsyncCall call = new Dispatcher.AsyncCall() {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                throw new HTTPClient.HTTPErrorException();
            }

            @Override
            void onError(Exception exception) {
                DispatcherTest.this.errorCalled = true;
            }
        };

        call.run();

        assertTrue(this.errorCalled);
    }

    private HTTPClient.Result result = null;
    @Test
    public void asyncCallHandlesSuccess() {
        final HTTPClient.Result result = new HTTPClient.Result();
        Dispatcher.AsyncCall call = new Dispatcher.AsyncCall() {
            @Override
            public HTTPClient.Result call() throws HTTPClient.HTTPErrorException {
                return result;
            }

            @Override
            void onCompletion(HTTPClient.Result result) {
                DispatcherTest.this.result = result;
            }
        };

        call.run();

        assertNotNull(this.result);
    }

}
