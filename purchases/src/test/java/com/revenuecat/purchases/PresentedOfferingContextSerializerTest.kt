package com.revenuecat.purchases

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PresentedOfferingContextSerializerTest {

    private val json = Json.Default

    @Test
    fun `round trips a presented offering context with a checkpoint identifier`() {
        val original = PresentedOfferingContext(
            offeringIdentifier = "offering-id",
            placementIdentifier = "placement-id",
            targetingContext = PresentedOfferingContext.TargetingContext(
                revision = 1,
                ruleId = "rule-id",
            ),
        ).copyWithCheckpointIdentifier("checkpoint-id")

        val encoded = json.encodeToString(PresentedOfferingContextSerializer, original)
        val decoded = json.decodeFromString(PresentedOfferingContextSerializer, encoded)
        val expectedJson = """
            {
                "offeringIdentifier":"offering-id",
                "placementIdentifier":"placement-id",
                "targetingContext":{"revision":1,"ruleId":"rule-id"},
                "checkpointIdentifier":"checkpoint-id"
            }
        """.trimIndent().lines().joinToString("") { it.trim() }

        assertThat(decoded).isEqualTo(original)
        assertThat(encoded).isEqualTo(expectedJson)
    }

    @Test
    fun `round trips a presented offering context with a trace id`() {
        val original = PresentedOfferingContext("offering-id")
            .copyWithCheckpointIdentifier("checkpoint-id")
            .copyWithTraceId("trace-id")

        val encoded = json.encodeToString(PresentedOfferingContextSerializer, original)
        val decoded = json.decodeFromString(PresentedOfferingContextSerializer, encoded)
        val expectedJson = """
            {
                "offeringIdentifier":"offering-id",
                "placementIdentifier":null,
                "targetingContext":null,
                "checkpointIdentifier":"checkpoint-id",
                "traceId":"trace-id"
            }
        """.trimIndent().lines().joinToString("") { it.trim() }

        assertThat(decoded).isEqualTo(original)
        assertThat(encoded).isEqualTo(expectedJson)
    }

    @Test
    fun `round trips a presented offering context without a checkpoint identifier`() {
        val original = PresentedOfferingContext(
            offeringIdentifier = "offering-id",
            placementIdentifier = "placement-id",
            targetingContext = PresentedOfferingContext.TargetingContext(
                revision = 1,
                ruleId = "rule-id",
            ),
        )

        val encoded = json.encodeToString(PresentedOfferingContextSerializer, original)
        val decoded = json.decodeFromString(PresentedOfferingContextSerializer, encoded)
        val expectedJson = """
            {
                "offeringIdentifier":"offering-id",
                "placementIdentifier":"placement-id",
                "targetingContext":{"revision":1,"ruleId":"rule-id"}
            }
        """.trimIndent().lines().joinToString("") { it.trim() }

        assertThat(decoded).isEqualTo(original)
        assertThat(encoded).isEqualTo(expectedJson)
    }
}
