package com.revenuecat.purchases.backend_integration_tests

import io.mockk.every
import org.junit.Before
import java.net.URL

internal class LoadShedderUSEast2BackendIntegrationTest: LoadShedderBackendIntegrationTest() {
    @Before
    fun setup() {
        every { appConfig.baseURL } returns URL("https://fortress2.revenuecat.com")
    }
}
