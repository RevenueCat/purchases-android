package com.revenuecat.purchases.ui.revenuecatui.paywalls

import android.os.Build
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
internal class PaywallImageLoaderTest {
    @Test
    @Config(sdk = [Build.VERSION_CODES.BAKLAVA])
    fun `OnePlus loader disables hardware bitmaps on Android 16`() {
        ShadowBuild.setBrand("OnePlus")

        val loader = PaywallImageLoader.get(getApplicationContext())

        assertThat(loader.defaults.allowHardware).isFalse()
    }

    @Test
    fun `Android 16 devices from affected brands use software bitmaps`() {
        val android16 = Build.VERSION_CODES.BAKLAVA
        assertThat(PaywallImageLoader.allowHardwareBitmaps(android16, "unknown", "OPPO")).isFalse()
        assertThat(PaywallImageLoader.allowHardwareBitmaps(android16, "realme", "realme")).isFalse()
    }

    @Test
    fun `other devices and Android versions keep hardware bitmap support`() {
        val android16 = Build.VERSION_CODES.BAKLAVA
        assertThat(PaywallImageLoader.allowHardwareBitmaps(android16, "google", "Google")).isTrue()
        assertThat(PaywallImageLoader.allowHardwareBitmaps(android16 - 1, "OPPO", "OPPO")).isTrue()
        assertThat(PaywallImageLoader.allowHardwareBitmaps(android16 + 1, "OPPO", "OPPO")).isTrue()
    }
}
