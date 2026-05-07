//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.PlatformInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.net.URL

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class PurchasesConfigureTest : BasePurchasesTest() {

    @Test
    fun `Setting platform info sets it in the AppConfig when configuring the SDK`() {
        val expected = PlatformInfo("flavor", "version")
        Purchases.platformInfo = expected
        Purchases.configureInCustomEntitlementsComputationMode(mockContext, "api", "appUserId")
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.platformInfo).isEqualTo(expected)
    }

    @Test
    fun `Setting proxy URL info sets it in the HttpClient when configuring the SDK`() {
        val expected = URL("https://a-proxy.com")
        Purchases.proxyURL = expected
        Purchases.configureInCustomEntitlementsComputationMode(mockContext, "api", "appUserId")
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `Configuring in custom entitlements mode sets dangerous settings`() {
        Purchases.configureInCustomEntitlementsComputationMode(mockContext, "api", "appUserId")
        val dangerousSettings = Purchases.sharedInstance.purchasesOrchestrator.appConfig.dangerousSettings
        assertThat(dangerousSettings.customEntitlementComputation).isTrue
        assertThat(dangerousSettings.applyObfuscatedAccountIdToSubscriptionChanges).isFalse
    }

    @Test
    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    fun `Configuring in custom entitlements mode propagates applyObfuscatedAccountIdToSubscriptionChanges`() {
        Purchases.configureInCustomEntitlementsComputationMode(
            PurchasesConfigurationForCustomEntitlementsComputationMode
                .Builder(mockContext, "api", "appUserId")
                .applyObfuscatedAccountIdToSubscriptionChanges(true)
                .build(),
        )

        assertThat(
            Purchases.sharedInstance.purchasesOrchestrator.appConfig.dangerousSettings
                .applyObfuscatedAccountIdToSubscriptionChanges,
        ).isTrue
    }
}
