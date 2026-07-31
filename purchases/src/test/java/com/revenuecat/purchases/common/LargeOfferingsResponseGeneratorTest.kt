package com.revenuecat.purchases.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LargeOfferingsResponseGeneratorTest {
    @Test
    fun `generates valid offerings-shaped JSON at or above the target size`() {
        val generated = LargeOfferingsResponseGenerator.generateAtLeast(64 * 1024)

        assertThat(generated.text.length).isGreaterThanOrEqualTo(64 * 1024)
        assertThat(generated.json.getJSONArray("offerings").length()).isEqualTo(1)
        assertThat(
            generated.json
                .getJSONArray("offerings")
                .getJSONObject(0)
                .getJSONObject("paywall_components")
                .getJSONObject("components_localizations")
                .getJSONObject("en_US")
                .getString("copy"),
        ).isNotEmpty()
    }
}
