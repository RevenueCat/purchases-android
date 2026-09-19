package com.revenuecat.purchases.common

import com.revenuecat.purchases.Store
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StorePlatformTest {

    @Test
    fun `amazon is its own platform`() {
        assertThat(Store.AMAZON.platformName).isEqualTo("amazon")
    }

    @Test
    fun `every other store is served by the android platform`() {
        val platformNames = Store.values().filterNot { it == Store.AMAZON }.map { it.platformName }

        assertThat(platformNames).isNotEmpty.containsOnly("android")
    }
}
