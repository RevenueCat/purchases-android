package com.revenuecat.purchases.ui.revenuecatui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallOptionsTest {

    @Test
    fun `options that differ only in dismissRequestWithExitOffering are not equal`() {
        // PaywallView stores its options in a structural-equality MutableState, so an equal copy is dropped.
        val options = PaywallOptions.Builder(dismissRequest = {}).build()

        val withExitOffering = options.copy(dismissRequestWithExitOffering = { _, _, _ -> })

        assertThat(withExitOffering).isNotEqualTo(options)
    }
}
