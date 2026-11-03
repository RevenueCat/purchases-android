@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

/**
 * Contains data about the context in which an offering was presented.
 */
@Parcelize
@Poko
public class PresentedOfferingContext private constructor(
    /**
     * The identifier of the offering used to obtain this object.
     */
    public val offeringIdentifier: String,
    /**
     * The identifier of the placement used to obtain this object.
     */
    public val placementIdentifier: String?,
    /**
     * The targeting context used to obtain this object.
     */
    public val targetingContext: TargetingContext?,
    @get:JvmSynthetic
    internal val checkpointIdentifier: String?,
    @get:JvmSynthetic
    @property:InternalRevenueCatAPI
    public val traceId: String?,
    @get:JvmSynthetic
    internal val workflowId: String?,
    @get:JvmSynthetic
    internal val stepId: String?,
    @get:JvmSynthetic
    internal val blobRef: String?,
) : Parcelable {
    // Preserve the existing public constructor signatures for binary compatibility.
    @Suppress("ForbiddenPublicConstructor")
    public constructor(
        offeringIdentifier: String,
        placementIdentifier: String?,
        targetingContext: TargetingContext?,
    ) : this(offeringIdentifier, placementIdentifier, targetingContext, null, null, null, null, null)

    @Suppress("ForbiddenPublicConstructor")
    public constructor(offeringIdentifier: String) : this(offeringIdentifier, null, null)

    @JvmSynthetic
    @InternalRevenueCatAPI
    public fun copy(
        offeringIdentifier: String = this.offeringIdentifier,
        placementIdentifier: String? = this.placementIdentifier,
        targetingContext: TargetingContext? = this.targetingContext,
    ): PresentedOfferingContext = PresentedOfferingContext(
        offeringIdentifier = offeringIdentifier,
        placementIdentifier = placementIdentifier,
        targetingContext = targetingContext,
        checkpointIdentifier = checkpointIdentifier,
        traceId = traceId,
        workflowId = workflowId,
        stepId = stepId,
        blobRef = blobRef,
    )

    @JvmSynthetic
    internal fun copyWithCheckpointIdentifier(checkpointIdentifier: String?): PresentedOfferingContext =
        PresentedOfferingContext(
            offeringIdentifier = offeringIdentifier,
            placementIdentifier = placementIdentifier,
            targetingContext = targetingContext,
            checkpointIdentifier = checkpointIdentifier,
            traceId = traceId,
            workflowId = workflowId,
            stepId = stepId,
            blobRef = blobRef,
        )

    @JvmSynthetic
    internal fun copyWithTraceId(traceId: String?): PresentedOfferingContext =
        PresentedOfferingContext(
            offeringIdentifier = offeringIdentifier,
            placementIdentifier = placementIdentifier,
            targetingContext = targetingContext,
            checkpointIdentifier = checkpointIdentifier,
            traceId = traceId,
            workflowId = workflowId,
            stepId = stepId,
            blobRef = blobRef,
        )

    @JvmSynthetic
    internal fun copyWithWorkflowContext(
        workflowId: String?,
        stepId: String?,
        blobRef: String?,
    ): PresentedOfferingContext = PresentedOfferingContext(
        offeringIdentifier = offeringIdentifier,
        placementIdentifier = placementIdentifier,
        targetingContext = targetingContext,
        checkpointIdentifier = checkpointIdentifier,
        traceId = traceId,
        workflowId = workflowId,
        stepId = stepId,
        blobRef = blobRef,
    )

    internal companion object {
        @Suppress("LongParameterList")
        fun create(
            offeringIdentifier: String,
            placementIdentifier: String?,
            targetingContext: TargetingContext?,
            checkpointIdentifier: String?,
            traceId: String?,
            workflowId: String?,
            stepId: String?,
            blobRef: String?,
        ): PresentedOfferingContext = PresentedOfferingContext(
            offeringIdentifier = offeringIdentifier,
            placementIdentifier = placementIdentifier,
            targetingContext = targetingContext,
            checkpointIdentifier = checkpointIdentifier,
            traceId = traceId,
            workflowId = workflowId,
            stepId = stepId,
            blobRef = blobRef,
        )
    }

    @Parcelize
    @Poko
    public class TargetingContext(
        /**
         * The revision of the targeting used to obtain this object.
         */
        public val revision: Int,

        /**
         * The rule id from the targeting used to obtain this object.
         */
        public val ruleId: String,
    ) : Parcelable
}

internal object TargetingContextSerializer : KSerializer<PresentedOfferingContext.TargetingContext> {
    private const val REVISION_INDEX = 0
    private const val RULE_ID_INDEX = 1

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TargetingContext") {
        element("revision", Int.serializer().descriptor)
        element("ruleId", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: PresentedOfferingContext.TargetingContext) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, REVISION_INDEX, value.revision)
            encodeStringElement(descriptor, RULE_ID_INDEX, value.ruleId)
        }
    }

    override fun deserialize(decoder: Decoder): PresentedOfferingContext.TargetingContext {
        return decoder.decodeStructure(descriptor) {
            var revision = 0
            var ruleId = ""

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    REVISION_INDEX -> revision = decodeIntElement(descriptor, REVISION_INDEX)
                    RULE_ID_INDEX -> ruleId = decodeStringElement(descriptor, RULE_ID_INDEX)
                    -1 -> break
                    else -> error("Unexpected index: $index")
                }
            }

            PresentedOfferingContext.TargetingContext(revision, ruleId)
        }
    }
}

internal object PresentedOfferingContextSerializer : KSerializer<PresentedOfferingContext> {
    private const val OFFERING_IDENTIFIER_INDEX = 0
    private const val PLACEMENT_IDENTIFIER_INDEX = 1
    private const val TARGETING_CONTEXT_INDEX = 2
    private const val CHECKPOINT_IDENTIFIER_INDEX = 3
    private const val TRACE_ID_INDEX = 4
    private const val WORKFLOW_ID_INDEX = 5
    private const val STEP_ID_INDEX = 6
    private const val BLOB_REF_INDEX = 7

    private val nullableStringSerializer = String.serializer().nullable
    private val nullableTargetingContextSerializer = TargetingContextSerializer.nullable

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PresentedOfferingContext") {
        element("offeringIdentifier", String.serializer().descriptor)
        element("placementIdentifier", nullableStringSerializer.descriptor)
        element("targetingContext", nullableTargetingContextSerializer.descriptor)
        element("checkpointIdentifier", nullableStringSerializer.descriptor, isOptional = true)
        element("traceId", nullableStringSerializer.descriptor, isOptional = true)
        element("workflowId", nullableStringSerializer.descriptor, isOptional = true)
        element("stepId", nullableStringSerializer.descriptor, isOptional = true)
        element("blobRef", nullableStringSerializer.descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: PresentedOfferingContext) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, OFFERING_IDENTIFIER_INDEX, value.offeringIdentifier)
            encodeSerializableElement(
                descriptor,
                PLACEMENT_IDENTIFIER_INDEX,
                nullableStringSerializer,
                value.placementIdentifier,
            )
            encodeSerializableElement(
                descriptor,
                TARGETING_CONTEXT_INDEX,
                nullableTargetingContextSerializer,
                value.targetingContext,
            )
            value.checkpointIdentifier?.let {
                encodeStringElement(descriptor, CHECKPOINT_IDENTIFIER_INDEX, it)
            }
            value.traceId?.let {
                encodeStringElement(descriptor, TRACE_ID_INDEX, it)
            }
            value.workflowId?.let {
                encodeStringElement(descriptor, WORKFLOW_ID_INDEX, it)
            }
            value.stepId?.let {
                encodeStringElement(descriptor, STEP_ID_INDEX, it)
            }
            value.blobRef?.let {
                encodeStringElement(descriptor, BLOB_REF_INDEX, it)
            }
        }
    }

    override fun deserialize(decoder: Decoder): PresentedOfferingContext {
        return decoder.decodeStructure(descriptor) {
            var offeringIdentifier = ""
            var placementIdentifier: String? = null
            var targetingContext: PresentedOfferingContext.TargetingContext? = null
            var checkpointIdentifier: String? = null
            var traceId: String? = null
            var workflowId: String? = null
            var stepId: String? = null
            var blobRef: String? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    OFFERING_IDENTIFIER_INDEX -> offeringIdentifier = decodeStringElement(
                        descriptor,
                        OFFERING_IDENTIFIER_INDEX,
                    )
                    PLACEMENT_IDENTIFIER_INDEX -> placementIdentifier = decodeSerializableElement(
                        descriptor,
                        PLACEMENT_IDENTIFIER_INDEX,
                        nullableStringSerializer,
                    )
                    TARGETING_CONTEXT_INDEX -> targetingContext = decodeSerializableElement(
                        descriptor,
                        TARGETING_CONTEXT_INDEX,
                        nullableTargetingContextSerializer,
                    )
                    CHECKPOINT_IDENTIFIER_INDEX -> checkpointIdentifier = decodeSerializableElement(
                        descriptor,
                        CHECKPOINT_IDENTIFIER_INDEX,
                        nullableStringSerializer,
                    )
                    TRACE_ID_INDEX -> traceId = decodeSerializableElement(
                        descriptor,
                        TRACE_ID_INDEX,
                        nullableStringSerializer,
                    )
                    WORKFLOW_ID_INDEX -> workflowId = decodeSerializableElement(
                        descriptor,
                        index,
                        nullableStringSerializer,
                    )
                    STEP_ID_INDEX -> stepId = decodeSerializableElement(descriptor, index, nullableStringSerializer)
                    BLOB_REF_INDEX -> blobRef = decodeSerializableElement(descriptor, index, nullableStringSerializer)
                    -1 -> break
                    else -> error("Unexpected index: $index")
                }
            }

            PresentedOfferingContext.create(
                offeringIdentifier,
                placementIdentifier,
                targetingContext,
                checkpointIdentifier,
                traceId,
                workflowId,
                stepId,
                blobRef,
            )
        }
    }
}
