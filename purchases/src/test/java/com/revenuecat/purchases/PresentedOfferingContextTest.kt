package com.revenuecat.purchases

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PresentedOfferingContextTest {

    @Test
    fun `context copies preserve workflow attribution without changing the original`() {
        val original = PresentedOfferingContext("offering-id")
            .copyWithWorkflowContext("workflow-id", "step-id", "blob-ref")
        val updated = original.copy(
            placementIdentifier = "placement-id",
            targetingContext = PresentedOfferingContext.TargetingContext(3, "rule-id"),
        ).copyWithCheckpointIdentifier("checkpoint-id").copyWithTraceId("trace-id")

        assertThat(updated.workflowId).isEqualTo("workflow-id")
        assertThat(updated.stepId).isEqualTo("step-id")
        assertThat(updated.blobRef).isEqualTo("blob-ref")
        assertThat(updated.placementIdentifier).isEqualTo("placement-id")
        assertThat(updated.checkpointIdentifier).isEqualTo("checkpoint-id")
        assertThat(updated.traceId).isEqualTo("trace-id")
        assertThat(original.placementIdentifier).isNull()
        assertThat(original.checkpointIdentifier).isNull()
        assertThat(original.traceId).isNull()
    }

    @Test
    fun `parcel roundtrip retains workflow attribution`() {
        val original = PresentedOfferingContext("offering")
            .copyWithWorkflowContext("workflow", "step", "blob-ref")
        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(original, 0)
            parcel.setDataPosition(0)
            @Suppress("DEPRECATION")
            val decoded = parcel.readParcelable<PresentedOfferingContext>(PresentedOfferingContext::class.java.classLoader)

            assertThat(decoded).isEqualTo(original)
        } finally {
            parcel.recycle()
        }
    }
}
