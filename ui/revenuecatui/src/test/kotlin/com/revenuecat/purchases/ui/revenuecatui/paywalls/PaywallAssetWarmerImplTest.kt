package com.revenuecat.purchases.ui.revenuecatui.paywalls

import com.revenuecat.purchases.paywalls.PaywallAssetWarmer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.ServiceLoader

internal class PaywallAssetWarmerImplTest {

    // Guards the META-INF/services descriptor against the impl being moved or renamed.
    @Test
    fun `warmer is discoverable through ServiceLoader`() {
        val loadedWarmer = ServiceLoader.load(
            PaywallAssetWarmer::class.java,
            PaywallAssetWarmer::class.java.classLoader,
        ).firstOrNull()

        assertThat(loadedWarmer).isInstanceOf(PaywallAssetWarmerImpl::class.java)
    }
}
