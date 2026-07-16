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
    fun `default applyObfuscatedAccountIdToSubscriptionChanges is false`() {
        val dangerousSettings = DangerousSettings()
        assertThat(dangerousSettings.applyObfuscatedAccountIdToSubscriptionChanges).isFalse
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

    @Test
    fun `default usesRemoteConfigAPISources is false`() {
        val dangerousSettings = DangerousSettings()
        assertThat(dangerousSettings.usesRemoteConfigAPISources).isFalse
    }

    @Test
    fun `usesRemoteConfigAPISources can be enabled`() {
        val dangerousSettings = DangerousSettings(usesRemoteConfigAPISources = true)
        assertThat(dangerousSettings.usesRemoteConfigAPISources).isTrue
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `forPreviewMode sets uiPreviewMode to true and autoSyncPurchases to false`() {
        val dangerousSettings = DangerousSettings.forPreviewMode()
        assertThat(dangerousSettings.uiPreviewMode).isTrue
        assertThat(dangerousSettings.autoSyncPurchases).isFalse
        assertThat(dangerousSettings.customEntitlementComputation).isFalse
        assertThat(dangerousSettings.applyObfuscatedAccountIdToSubscriptionChanges).isFalse
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `default useWorkflows is false`() {
        val dangerousSettings = DangerousSettings()
        assertThat(dangerousSettings.useWorkflows).isFalse
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `forWorkflows sets useWorkflows to true and leaves other settings at defaults`() {
        val dangerousSettings = DangerousSettings.forWorkflows()
        assertThat(dangerousSettings.useWorkflows).isTrue
        assertThat(dangerousSettings.autoSyncPurchases).isTrue
        assertThat(dangerousSettings.customEntitlementComputation).isFalse
        assertThat(dangerousSettings.uiPreviewMode).isFalse
        assertThat(dangerousSettings.applyObfuscatedAccountIdToSubscriptionChanges).isFalse
    }
}
