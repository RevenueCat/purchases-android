package com.revenuecat.purchases.galaxy.constants

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalaxyConsumeOrAcknowledgeStatusCodeTest {

    @Test
    fun `fromCode maps known galaxy consume or acknowledge status codes`() {
        val expectedMappings = mapOf(
            GalaxyConsumeOrAcknowledgeStatusCode.SUCCESS.code to GalaxyConsumeOrAcknowledgeStatusCode.SUCCESS,
            GalaxyConsumeOrAcknowledgeStatusCode.INVALID_PURCHASE_ID.code to GalaxyConsumeOrAcknowledgeStatusCode.INVALID_PURCHASE_ID,
            GalaxyConsumeOrAcknowledgeStatusCode.FAILED_ORDER.code to GalaxyConsumeOrAcknowledgeStatusCode.FAILED_ORDER,
            GalaxyConsumeOrAcknowledgeStatusCode.INVALID_PRODUCT_TYPE.code to GalaxyConsumeOrAcknowledgeStatusCode.INVALID_PRODUCT_TYPE,
            GalaxyConsumeOrAcknowledgeStatusCode.ALREADY_CONSUMED_OR_ACKNOWLEDGED.code to GalaxyConsumeOrAcknowledgeStatusCode.ALREADY_CONSUMED_OR_ACKNOWLEDGED,
            GalaxyConsumeOrAcknowledgeStatusCode.UNAUTHORIZED_USER.code to GalaxyConsumeOrAcknowledgeStatusCode.UNAUTHORIZED_USER,
            GalaxyConsumeOrAcknowledgeStatusCode.UNEXPECTED_SERVICE_ERROR.code to GalaxyConsumeOrAcknowledgeStatusCode.UNEXPECTED_SERVICE_ERROR,
        )

        assertThat(expectedMappings).hasSize(GalaxyConsumeOrAcknowledgeStatusCode.values().size)

        GalaxyConsumeOrAcknowledgeStatusCode.values().forEach { status ->
            val mappedStatus = GalaxyConsumeOrAcknowledgeStatusCode.fromCode(status.code)

            assertThat(mappedStatus)
                .describedAs("Galaxy consume/ack code ${status.code} should map to ${status.name}")
                .isEqualTo(expectedMappings.getValue(status.code))
        }
    }

    @Test
    fun `fromCode returns null for unknown codes`() {
        val mappedStatus = GalaxyConsumeOrAcknowledgeStatusCode.fromCode(-999)
        assertThat(mappedStatus).isNull()
    }
}
