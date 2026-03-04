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

    @Test
    fun `default uiPreviewMode is false`() {
        val dangerousSettings = DangerousSettings()
        assertThat(dangerousSettings.uiPreviewMode).isFalse
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `forPreviewMode sets uiPreviewMode to true and autoSyncPurchases to false`() {
        val dangerousSettings = DangerousSettings.forPreviewMode()
        assertThat(dangerousSettings.uiPreviewMode).isTrue
        assertThat(dangerousSettings.autoSyncPurchases).isFalse
        assertThat(dangerousSettings.customEntitlementComputation).isFalse
    }
}
