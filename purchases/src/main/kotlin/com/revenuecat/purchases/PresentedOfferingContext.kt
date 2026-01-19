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
class PresentedOfferingContext @JvmOverloads constructor(
    /**
     * The identifier of the offering used to obtain this object.
     */
    val offeringIdentifier: String,
    /**
     * The identifier of the placement used to obtain this object.
     */
    val placementIdentifier: String?,
    /**
     * The targeting context used to obtain this object.
     */
    val targetingContext: TargetingContext?,
) : Parcelable {
    constructor(offeringIdentifier: String) : this(offeringIdentifier, null, null)

    @JvmSynthetic
    internal fun copy(
        offeringIdentifier: String = this.offeringIdentifier,
        placementIdentifier: String? = this.placementIdentifier,
        targetingContext: TargetingContext? = this.targetingContext,
    ): PresentedOfferingContext = PresentedOfferingContext(
        offeringIdentifier = offeringIdentifier,
        placementIdentifier = placementIdentifier,
        targetingContext = targetingContext,
    )

    @Parcelize
    @Poko
    class TargetingContext(
        /**
         * The revision of the targeting used to obtain this object.
         */
        val revision: Int,

        /**
         * The rule id from the targeting used to obtain this object.
         */
        val ruleId: String,
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

    private val nullableStringSerializer = String.serializer().nullable
    private val nullableTargetingContextSerializer = TargetingContextSerializer.nullable

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PresentedOfferingContext") {
        element("offeringIdentifier", String.serializer().descriptor)
        element("placementIdentifier", nullableStringSerializer.descriptor)
        element("targetingContext", nullableTargetingContextSerializer.descriptor)
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
        }
    }

    override fun deserialize(decoder: Decoder): PresentedOfferingContext {
        return decoder.decodeStructure(descriptor) {
            var offeringIdentifier = ""
            var placementIdentifier: String? = null
            var targetingContext: PresentedOfferingContext.TargetingContext? = null

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
                    -1 -> break
                    else -> error("Unexpected index: $index")
                }
            }

            PresentedOfferingContext(offeringIdentifier, placementIdentifier, targetingContext)
        }
    }
}
