package com.revenuecat.purchases

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import com.revenuecat.purchases.common.attribution.AttributionNetwork as CommonAttributionNetwork

class AttributionNetworkConversionTests {

    @Test
    fun `all AttributionNetwork can be converted`() {
        val converted: List<CommonAttributionNetwork> = Purchases.AttributionNetwork.values().map {
            it.convert()
        }
        assertThat(converted.size).isEqualTo(Purchases.AttributionNetwork.values().size)
    }

    @Test
    fun `ADJUST can be converted`() {
        assertThat(Purchases.AttributionNetwork.ADJUST.convert()).isEqualTo(CommonAttributionNetwork.ADJUST)
    }

    @Test
    fun `APPSFLYER can be converted`() {
        assertThat(Purchases.AttributionNetwork.APPSFLYER.convert()).isEqualTo(CommonAttributionNetwork.APPSFLYER)
    }

    @Test
    fun `BRANCH can be converted`() {
        assertThat(Purchases.AttributionNetwork.BRANCH.convert()).isEqualTo(CommonAttributionNetwork.BRANCH)
    }

    @Test
    fun `TENJIN can be converted`() {
        assertThat(Purchases.AttributionNetwork.TENJIN.convert()).isEqualTo(CommonAttributionNetwork.TENJIN)
    }

    @Test
    fun `FACEBOOK can be converted`() {
        assertThat(Purchases.AttributionNetwork.FACEBOOK.convert()).isEqualTo(CommonAttributionNetwork.FACEBOOK)
    }

    @Test
    fun `MPARTICLE can be converted`() {
        assertThat(Purchases.AttributionNetwork.MPARTICLE.convert()).isEqualTo(CommonAttributionNetwork.MPARTICLE)
    }

    @Test
    fun `same size of enums`() {
        assertThat(Purchases.AttributionNetwork.values().size).isEqualTo(CommonAttributionNetwork.values().size)
    }
}
