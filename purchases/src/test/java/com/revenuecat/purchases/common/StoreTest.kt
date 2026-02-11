package com.revenuecat.purchases.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.Store.values
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public class StoreTest {
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

    @Test
    fun `serialization defaults to UNKNOWN_STORE for unknown values`() {
        @Serializable
        public data class TestWrapper(@SerialName("store") val store: Store)

        val json = JsonTools.json

        val stringValueByEnumValue: Map<Store, String> = values().associateWith { store -> store.stringValue }

        // Test with known values
        stringValueByEnumValue.forEach { store, stringValue ->
            val jsonString = """{"store":"$stringValue"}"""
            val parsed = json.decodeFromString<TestWrapper>(jsonString)
            assertThat(parsed.store).isEqualTo(store)
        }

        // Test with an unknown value - should default to UNKNOWN_STORE
        val unknownStore = json.decodeFromString<TestWrapper>("""{"store":"invalid_store"}""")
        assertThat(unknownStore.store).isEqualTo(Store.UNKNOWN_STORE)
    }
}
