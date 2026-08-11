package com.revenuecat.purchases.common.audiences

import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class AudiencesConfigProviderTest {
    @Test
    fun `audiences use the backend audiences topic`() {
        assertThat(RemoteConfigTopic.Audiences.wireName).isEqualTo("audiences")
    }
}
