package com.revenuecat.purchases.backend_integration_tests

import io.mockk.every
import org.junit.Before
import java.net.URL

internal class LoadShedderUSEast2BackendIntegrationTest: LoadShedderUSEast1BackendIntegrationTest() {
    @Before
    override fun setup() {
        every { appConfig.baseURL } returns URL("https://fortress-us-east-2.revenuecat.com/")
    }
}
