package com.revenuecat.purchases.galaxy

import android.os.Handler
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesStateProvider
import com.revenuecat.purchases.common.currentLogHandler
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.fail

class GalaxyBillingWrapperTest {

    private val stateProvider = mockk<PurchasesStateProvider>(relaxed = true)
    private var handler = mockk<Handler>()
    private lateinit var wrapper: GalaxyBillingWrapper
    private var previousLogHandler: LogHandler? = null

    @Before
    fun setUp() {
        previousLogHandler = currentLogHandler
        currentLogHandler = mockk(relaxed = true)

        wrapper = GalaxyBillingWrapper(
            stateProvider,
            mainHandler = handler,
            billingMode = GalaxyBillingMode.TEST
        )
    }

    @After
    fun tearDown() {
        previousLogHandler?.let { currentLogHandler = it }
    }

    @Test
    fun `getStorefront returns unsupported error`() {
        var receivedError: PurchasesError? = null

        wrapper.getStorefront(
            onSuccess = { fail("Expected getStorefront to be unsupported") },
            onError = { receivedError = it },
        )

        assertThat(receivedError?.code).isEqualTo(PurchasesErrorCode.UnsupportedError)
        assertThat(receivedError?.underlyingErrorMessage).isEqualTo(GalaxyStrings.STOREFRONT_NOT_SUPPORTED)
    }
}
