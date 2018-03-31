package com.revenuecat.purchases;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

class BackendTest {
    private HTTPClient mockClient;
    private Dispatcher mockDispatcher;
    private Backend backend;
    private String API_KEY = "TEST_API_KEY";

    @Before
    public void setup() {
        mockClient = mock(HTTPClient.class);
        mockDispatcher = mock(Dispatcher.class);

        backend = new Backend(API_KEY, mockDispatcher, mockClient);
    }

    @Test
    public void canBeCreated() {
        assertNotNull(backend);
    }
}
