package com.revenuecat.purchases.common

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.PurchasesAreCompletedBy.MY_APP
import com.revenuecat.purchases.PurchasesAreCompletedBy.REVENUECAT
import com.revenuecat.purchases.Store
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AppConfigTest {

    @After
    fun tearDown() {
        unmockkStatic("com.revenuecat.purchases.common.UtilsKt")
    }

    @Test
    fun `languageTag is created successfully`() {
        val expected = "en-US"
        val mockContext = mockk<Context>(relaxed = true)
        every {
            mockContext.getLocale()?.toLanguageTag()
        } returns expected
        val appConfig = AppConfig(
            context = mockContext,
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.languageTag).isEqualTo(expected)
    }

    @Test
    fun `languageTag defaults to empty string`() {
        val expected = ""
        val mockContext = mockk<Context>(relaxed = true)
        every {
            mockContext.getLocale()?.toLanguageTag()
        } returns null
        val appConfig = AppConfig(
            context = mockContext,
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.languageTag).isEqualTo(expected)
    }

    @Test
    fun `versionName is created successfully`() {
        val expected = "1.0.0"
        mockkStatic("com.revenuecat.purchases.common.UtilsKt")
        val mockContext = mockk<Context>(relaxed = true) {
            every {
                versionName
            } returns expected
        }

        val appConfig = AppConfig(
            context = mockContext,
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.versionName).isEqualTo(expected)
    }

    @Test
    fun `versionName defaults to empty string`() {
        val expected = ""
        mockkStatic("com.revenuecat.purchases.common.UtilsKt")
        val mockContext = mockk<Context>(relaxed = true) {
            every {
                versionName
            } returns null
        }
        val appConfig = AppConfig(
            context = mockContext,
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.versionName).isEqualTo(expected)
    }

    @Test
    fun `packageName is created successfully`() {
        val expected = "com.revenuecat.test-package-name"
        mockkStatic("com.revenuecat.purchases.common.UtilsKt")
        val mockContext = mockk<Context>(relaxed = true) {
            every {
                packageName
            } returns expected
        }

        val appConfig = AppConfig(
            context = mockContext,
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.packageName).isEqualTo(expected)
    }

    @Test
    fun `showInAppMessagesAutomatically is set correctly`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.showInAppMessagesAutomatically).isFalse
        val appConfig2 = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = true,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig2.showInAppMessagesAutomatically).isTrue
    }

    @Test
    fun `finishTransactions is set correctly when observer mode is false`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.finishTransactions).isTrue()
    }

    @Test
    fun `finishTransactions is set correctly when observer mode is true`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = MY_APP,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.finishTransactions).isFalse()
    }

    @Test
    fun `proxyURL is set as a baseURL`() {
        val expected = URL("https://a-proxy")
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = expected,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `default baseURL is correct`() {
        val expected = URL("https://api.revenuecat.com/")
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `default forceServerErrors is correct`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.forceServerErrors).isFalse
    }

    @Test
    fun `default forceSigningErrors is correct`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.forceSigningErrors).isFalse
    }

    @Test
    fun `default isAppBackgrounded is correct`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.isAppBackgrounded).isTrue
    }

    @Test
    fun `isAppBackgrounded can be modified correctly`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.isAppBackgrounded).isTrue
        appConfig.isAppBackgrounded = false
        assertThat(appConfig.isAppBackgrounded).isFalse
    }

    @Test
    fun `customEntitlementComputation matches version from dangerous settings`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
            dangerousSettings = DangerousSettings(customEntitlementComputation = true)
        )
        assertThat(appConfig.customEntitlementComputation).isTrue
        val appConfig2 = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
            dangerousSettings = DangerousSettings(customEntitlementComputation = false)
        )
        assertThat(appConfig2.customEntitlementComputation).isFalse
    }

    @Test
    fun `Given two app configs with same data, both are equal`() {
        val x = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        val y = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )

        assertThat(x).isEqualTo(y)
    }

    @Test
    fun `Given two app configs with different data, both are not equal`() {
        val x = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        var y = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = MY_APP,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )

        assertThat(x).isNotEqualTo(y)

        y = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.1.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )

        assertThat(x).isNotEqualTo(y)

        y = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = URL("https://a.com"),
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )

        assertThat(x).isNotEqualTo(y)

        y = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
            dangerousSettings = DangerousSettings(autoSyncPurchases = false)
        )

        assertThat(x).isNotEqualTo(y)

        y = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = true,
        )

        assertThat(x).isNotEqualTo(y)
    }

    @Test
    fun `Given two same app configs, their hashcodes are the same`() {
        val x = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        val y = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(x.hashCode()).isEqualTo(y.hashCode())
    }

    @Test
    fun `toString works`() {
        val x = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(x.toString()).isEqualTo(
            "AppConfig(" +
                "platformInfo=PlatformInfo(flavor=native, version=3.2.0), " +
                "store=PLAY_STORE, " +
                "isDebugBuild=false, " +
                "dangerousSettings=DangerousSettings(autoSyncPurchases=true, customEntitlementComputation=false), " +
                "languageTag='', " +
                "versionName='', " +
                "packageName='', " +
                "finishTransactions=true, " +
                "showInAppMessagesAutomatically=false, " +
                "baseURL=https://api.revenuecat.com/)")
    }

    // region Fallback API host

    @Test
    fun `appConfig returns expected fallback URLs list when no proxy URL is set`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.fallbackBaseURLs).isEqualTo(listOf(URL("https://api-production.8-lives-cat.io/")))
    }

    @Test
    fun `appConfig returns empty fallback URLs list when a proxy URL is set`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            purchasesAreCompletedBy = REVENUECAT,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = URL("https://proxy.com"),
            store = Store.PLAY_STORE,
            isDebugBuild = false,
        )
        assertThat(appConfig.fallbackBaseURLs).isEmpty()
    }

    // endregion Fallback API host
}
