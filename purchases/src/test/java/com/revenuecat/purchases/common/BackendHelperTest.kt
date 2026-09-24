package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.networking.Endpoint
import com.revenuecat.purchases.common.networking.HTTPResult
import com.revenuecat.purchases.common.networking.RCHTTPStatusCodes
import com.revenuecat.purchases.common.networking.RequestLane
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class BackendHelperTest {

    @Test
    fun `performRequest enqueues on the dispatcher of the endpoint's lane`() {
        val defaultDispatcher = mockk<Dispatcher>(relaxed = true)
        val eventsDispatcher = mockk<Dispatcher>(relaxed = true)
        val lanes = BackendLanes(defaultDispatcher, mapOf(RequestLane.EVENTS to eventsDispatcher))
        val backendHelper = BackendHelper("TEST_API_KEY", lanes, mockk(), mockk())

        backendHelper.performRequest(
            Endpoint.PostAttributes("test-user-id"),
            body = null,
            postFieldsToSign = null,
            delay = Delay.NONE,
            onError = {},
            onCompleted = { _, _, _ -> },
        )
        verify(exactly = 1) { defaultDispatcher.enqueue(any(), Delay.NONE) }
        verify(exactly = 0) { eventsDispatcher.enqueue(any(), any()) }

        backendHelper.performRequest(
            Endpoint.PostDiagnostics,
            body = null,
            postFieldsToSign = null,
            delay = Delay.NONE,
            onError = {},
            onCompleted = { _, _, _ -> },
        )
        verify(exactly = 1) { eventsDispatcher.enqueue(any(), Delay.NONE) }
        verify(exactly = 1) { defaultDispatcher.enqueue(any(), any()) }
    }

    @Test
    fun `HTTPResult isSuccessful is true only for 2xx codes`() {
        listOf(RCHTTPStatusCodes.SUCCESS, RCHTTPStatusCodes.CREATED, RCHTTPStatusCodes.NO_CONTENT, 299).forEach { code ->
            assertThat(HTTPResult.createResult(responseCode = code).isSuccessful()).describedAs("code $code").isTrue
        }
    }

    @Test
    fun `HTTPResult isSuccessful is false for 3xx and error codes`() {
        listOf(
            RCHTTPStatusCodes.UNSUCCESSFUL,
            RCHTTPStatusCodes.NOT_MODIFIED,
            RCHTTPStatusCodes.BAD_REQUEST,
            RCHTTPStatusCodes.NOT_FOUND,
            RCHTTPStatusCodes.ERROR,
        ).forEach { code ->
            assertThat(HTTPResult.createResult(responseCode = code).isSuccessful()).describedAs("code $code").isFalse
        }
    }

    @Test
    fun `HTTPResult isSuccessful is false below 200`() {
        listOf(HTTPClient.NO_STATUS_CODE, 0, 100, 199).forEach { code ->
            assertThat(HTTPResult.createResult(responseCode = code).isSuccessful()).describedAs("code $code").isFalse
        }
    }
}
