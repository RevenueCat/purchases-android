package com.revenuecat.purchases

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL

@RunWith(AndroidJUnit4::class)
class AppConfigTest {

    @Test
    fun `languageTag is created successfully`() {
        val expected = "en-US"
        val mockContext = mockk<Context>(relaxed = true)
        mockkStatic("com.revenuecat.purchases.UtilsKt")
        every {
            mockContext.getLocale()?.toBCP47()
        } returns expected
        val appConfig = AppConfig(
            context = mockContext,
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(appConfig.languageTag).isEqualTo(expected)
    }

    @Test
    fun `languageTag defaults to empty string`() {
        val expected = ""
        val mockContext = mockk<Context>(relaxed = true)
        mockkStatic("com.revenuecat.purchases.UtilsKt")
        every {
            mockContext.getLocale()?.toBCP47()
        } returns null
        val appConfig = AppConfig(
            context = mockContext,
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(appConfig.languageTag).isEqualTo(expected)
    }

    @Test
    fun `versionName is created successfully`() {
        val expected = "1.0.0"
        mockkStatic("com.revenuecat.purchases.UtilsKt")
        val mockContext = mockk<Context>(relaxed = true) {
            every {
                versionName
            } returns expected
        }

        val appConfig = AppConfig(
            context = mockContext,
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(appConfig.versionName).isEqualTo(expected)
    }

    @Test
    fun `versionName defaults to empty string`() {
        val expected = ""
        mockkStatic("com.revenuecat.purchases.UtilsKt")
        val mockContext = mockk<Context>(relaxed = true) {
            every {
                versionName
            } returns null
        }
        val appConfig = AppConfig(
            context = mockContext,
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(appConfig.versionName).isEqualTo(expected)
    }

    @Test
    fun `finishTransactions is set correctly when observer mode is false`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(appConfig.finishTransactions).isTrue()
    }

    @Test
    fun `finishTransactions is set correctly when observer mode is true`() {
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            observerMode = true,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(appConfig.finishTransactions).isFalse()
    }

    @Test
    fun `proxyURL is set as a baseURL`() {
        val expected = URL("https://a-proxy")
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = expected
        )
        assertThat(appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `default baseURL is correct`() {
        val expected = URL("https://api.revenuecat.com/")
        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(appConfig.baseURL).isEqualTo(expected)
    }

    @Test
    fun `Given two app configs with same data, both are equal`() {
        val x = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        val y = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )

        assertThat(x).isEqualTo(y)
    }

    @Test
    fun `Given two app configs with different data, both are not equal`() {
        val x = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        var y = AppConfig(
            context = mockk(relaxed = true),
            observerMode = true,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )

        assertThat(x).isNotEqualTo(y)

        y = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.1.0"),
            proxyURL = null
        )

        assertThat(x).isNotEqualTo(y)

        y = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = URL("https://a.com")
        )

        assertThat(x).isNotEqualTo(y)
    }

    @Test
    fun `Given two same app configs, their hashcodes are the same`() {
        val x = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        val y = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(x.hashCode() == y.hashCode())
    }

    @Test
    fun `toString works`() {
        val x = AppConfig(
            context = mockk(relaxed = true),
            observerMode = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null
        )
        assertThat(x.toString()).isNotNull()
    }
}