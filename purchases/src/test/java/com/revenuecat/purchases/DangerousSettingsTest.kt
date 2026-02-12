package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DangerousSettingsTest {
    @Test
    fun `default customEntitlementComputation is false`() {
        val dangerousSettings = DangerousSettings()
        assertThat(dangerousSettings.customEntitlementComputation).isFalse
    }

    @Test
    fun `default autoSyncPurchases is true`() {
        val dangerousSettings = DangerousSettings()
        assertThat(dangerousSettings.autoSyncPurchases).isTrue
    }
}
