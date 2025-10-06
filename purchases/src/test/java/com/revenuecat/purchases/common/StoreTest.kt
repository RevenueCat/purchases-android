package com.revenuecat.purchases.common

import com.revenuecat.purchases.Store
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StoreTest {
    @Test
    fun `can parse all defined stores`() {
        assertThat(Store.fromString("app_store")).isEqualTo(Store.APP_STORE)
        assertThat(Store.fromString("mac_app_store")).isEqualTo(Store.MAC_APP_STORE)
        assertThat(Store.fromString("play_store")).isEqualTo(Store.PLAY_STORE)
        assertThat(Store.fromString("stripe")).isEqualTo(Store.STRIPE)
        assertThat(Store.fromString("promotional")).isEqualTo(Store.PROMOTIONAL)
        assertThat(Store.fromString("amazon")).isEqualTo(Store.AMAZON)
        assertThat(Store.fromString("rc_billing")).isEqualTo(Store.RC_BILLING)
        assertThat(Store.fromString("external")).isEqualTo(Store.EXTERNAL)
        assertThat(Store.fromString("paddle")).isEqualTo(Store.PADDLE)
        assertThat(Store.fromString("test_store")).isEqualTo(Store.TEST_STORE)
        assertThat(Store.fromString("unknown")).isEqualTo(Store.UNKNOWN_STORE)
        assertThat(Store.fromString("invalid_store")).isEqualTo(Store.UNKNOWN_STORE)
    }
}
