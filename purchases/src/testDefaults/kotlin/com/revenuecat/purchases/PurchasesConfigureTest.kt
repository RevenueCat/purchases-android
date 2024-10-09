//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PurchasesAreCompletedBy.MY_APP
import com.revenuecat.purchases.PurchasesAreCompletedBy.REVENUECAT
import com.revenuecat.purchases.common.PlatformInfo
import io.mockk.every
import io.mockk.mockk
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
        Purchases.configure(PurchasesConfiguration.Builder(mockContext, "api").build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.platformInfo).isEqualTo(expected)
    }

    @Test
    fun `Setting proxy URL info sets it in the HttpClient when configuring the SDK`() {
        val expected = URL("https://a-proxy.com")
        Purchases.proxyURL = expected
        Purchases.configure(PurchasesConfiguration.Builder(mockContext, "api").build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `Setting observer mode on sets finish transactions to false`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").observerMode(true)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.finishTransactions).isFalse
    }

    @Test
    fun `Setting observer mode off sets finish transactions to true`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").observerMode(false)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.finishTransactions).isTrue
    }

    @Test
    fun `Setting purchasesAreCompletedBy REVENUECAT propagates to appConfig`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").purchasesAreCompletedBy(REVENUECAT)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.finishTransactions).isTrue
    }

    @Test
    fun `Setting purchasesAreCompletedBy MY_APP propagates to appConfig`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").purchasesAreCompletedBy(MY_APP)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.purchasesOrchestrator.appConfig.finishTransactions).isFalse
    }

    @Test
    fun `Setting store in the configuration sets it on the Purchases instance`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api").store(Store.PLAY_STORE)
        Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance.store).isEqualTo(Store.PLAY_STORE)
    }

    @Test
    fun `Calling configure multiple times with same configuration does not create a new instance`() {
        val builder = PurchasesConfiguration.Builder(mockContext, "api_key")
        val instance1 = Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance).isEqualTo(instance1)
        val instance2 = Purchases.configure(builder.build())
        assertThat(Purchases.sharedInstance).isEqualTo(instance2)
        assertThat(instance2).isEqualTo(instance1)
    }

    @Test
    fun `Calling configure multiple times with different configuration does create a new instance`() {
        val config1 = PurchasesConfiguration.Builder(mockContext, "api_key").build()
        val instance1 = Purchases.configure(config1)
        assertThat(Purchases.sharedInstance).isEqualTo(instance1)
        val config2 = PurchasesConfiguration.Builder(mockContext, "api_key2").build()
        val instance2 = Purchases.configure(config2)
        assertThat(Purchases.sharedInstance).isEqualTo(instance2)
        assertThat(instance2).isNotEqualTo(instance1)
    }

    @Test
    fun `configurations with same properties are equal`() {
        val config1 = PurchasesConfiguration.Builder(mockContext, "api_key").build()
        val config2 = PurchasesConfiguration.Builder(mockContext, "api_key").build()

        assertThat(config1).isEqualTo(config2)
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
    }

    @Test
    fun `configurations with different api keys are not equal`() {
        val config1 = PurchasesConfiguration.Builder(mockContext, "api_key1").build()
        val config2 = PurchasesConfiguration.Builder(mockContext, "api_key2").build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun `configurations with different app user IDs are not equal`() {
        val config1 = PurchasesConfiguration.Builder(mockContext, "api_key").appUserID("user1").build()
        val config2 = PurchasesConfiguration.Builder(mockContext, "api_key").appUserID("user2").build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun `configurations with different purchasesAreCompletedBy are not equal`() {
        val config1 = PurchasesConfiguration.Builder(mockContext, "api_key").purchasesAreCompletedBy(MY_APP).build()
        val config2 = PurchasesConfiguration.Builder(mockContext, "api_key").purchasesAreCompletedBy(REVENUECAT).build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun `configurations with different stores are not equal`() {
        val config1 = PurchasesConfiguration.Builder(mockContext, "api_key").store(Store.PLAY_STORE).build()
        val config2 = PurchasesConfiguration.Builder(mockContext, "api_key").store(Store.AMAZON).build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun `configurations with different contexts are equal`() {
        val context1 = mockContext
        val context2 = mockk<Context>().apply { every { applicationContext } returns mockk() }
        val config1 = PurchasesConfiguration.Builder(context1, "api_key").build()
        val config2 = PurchasesConfiguration.Builder(context2, "api_key").build()

        assertThat(config1).isEqualTo(config2)
    }

    @Test
    fun `configurations with different verificationMode are not equal`() {
        val config1 = PurchasesConfiguration.Builder(mockContext, "api_key")
            .entitlementVerificationMode(EntitlementVerificationMode.DISABLED)
            .build()
        val config2 = PurchasesConfiguration.Builder(mockContext, "api_key")
            .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
            .build()

        assertThat(config1).isNotEqualTo(config2)
    }

    @Test
    fun `configurations with different dangerousSettings are not equal`() {
        val config1 = PurchasesConfiguration.Builder(mockContext, "api_key")
            .dangerousSettings(DangerousSettings(autoSyncPurchases = true))
            .build()
        val config2 = PurchasesConfiguration.Builder(mockContext, "api_key")
            .dangerousSettings(DangerousSettings(autoSyncPurchases = false))
            .build()

        assertThat(config1).isNotEqualTo(config2)
    }
}
