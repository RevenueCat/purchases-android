package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.networking.Endpoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@OptIn(InternalRevenueCatAPI::class)
@RunWith(AndroidJUnit4::class)
class SimulatedHostFailuresTest {

    private val endpoint = Endpoint.GetCustomerInfo("user")
    private var current: SimulatedHostFailures? = SimulatedHostFailures(
        serverErrorHosts = setOf("erroring.revenuecat.com"),
        unreachableHosts = setOf("down.revenuecat.com"),
    )
    private val strategy = SimulatedHostFailures.strategy { current }

    @Test
    fun `server error hosts get server errors but stay reachable`() {
        assertThat(strategy.shouldForceServerError(URL("https://erroring.revenuecat.com/"), endpoint)).isTrue
        assertThat(strategy.shouldForceConnectionFailure("https://erroring.revenuecat.com/v1/health/connectivity"))
            .isFalse
    }

    @Test
    fun `unreachable hosts get server errors and fail to connect`() {
        assertThat(strategy.shouldForceServerError(URL("https://down.revenuecat.com/"), endpoint)).isTrue
        assertThat(strategy.shouldForceConnectionFailure("https://down.revenuecat.com/v1/health/connectivity")).isTrue
    }

    @Test
    fun `other hosts are untouched`() {
        assertThat(strategy.shouldForceServerError(URL("https://api.revenuecat.com/"), endpoint)).isFalse
        assertThat(strategy.shouldForceConnectionFailure("https://api.revenuecat.com/v1/health/connectivity")).isFalse
    }

    @Test
    fun `changes after the strategy is built apply to the next request`() {
        current = SimulatedHostFailures(serverErrorHosts = emptySet(), unreachableHosts = setOf("api.revenuecat.com"))

        assertThat(strategy.shouldForceServerError(URL("https://api.revenuecat.com/"), endpoint)).isTrue
        assertThat(strategy.shouldForceConnectionFailure("https://api.revenuecat.com/v1/health/connectivity")).isTrue
        assertThat(strategy.shouldForceServerError(URL("https://down.revenuecat.com/"), endpoint)).isFalse
    }

    @Test
    fun `a null value fails nothing`() {
        current = null

        assertThat(strategy.shouldForceServerError(URL("https://down.revenuecat.com/"), endpoint)).isFalse
        assertThat(strategy.shouldForceConnectionFailure("https://down.revenuecat.com/v1/health/connectivity")).isFalse
    }
}
