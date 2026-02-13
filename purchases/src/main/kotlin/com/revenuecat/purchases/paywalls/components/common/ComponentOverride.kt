package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride.Condition
import com.revenuecat.purchases.utils.serializers.SealedDeserializerWithDefault
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@InternalRevenueCatAPI
@Poko
@Serializable
public class ComponentOverride<T : PartialComponent>(
    @get:JvmSynthetic public val conditions: List<Condition>,
    @get:JvmSynthetic public val properties: T,
) {

    @Serializable
    public enum class EqualityOperator {
        @SerialName("=")
        EQUALS,

        @SerialName("!=")
        NOT_EQUALS,
    }

    @Serializable
    public enum class ArrayOperator {
        @SerialName("in")
        IN,

        @SerialName("not in")
        NOT_IN,
    }

    @Serializable(with = ConditionValueSerializer::class)
    public sealed interface ConditionValue {

        @JvmInline
        public value class StringValue(public val value: String) : ConditionValue

        @JvmInline
        public value class IntValue(public val value: Int) : ConditionValue

        @JvmInline
        public value class DoubleValue(public val value: Double) : ConditionValue

        @JvmInline
        public value class BoolValue(public val value: Boolean) : ConditionValue
    }

    @Serializable(with = ConditionSerializer::class)
    public sealed interface Condition {
        @Serializable
        public object Compact : Condition

        @Serializable
        public object Medium : Condition

        @Serializable
        public object Expanded : Condition

        @Serializable
        public data class IntroOffer(
            public val operator: EqualityOperator? = null,
            public val value: Boolean? = null,
        ) : Condition

        @Serializable
        public object MultiplePhaseOffers : Condition

        @Serializable
        public object Selected : Condition

        @Serializable
        public data class PromoOffer(
            public val operator: EqualityOperator? = null,
            public val value: Boolean? = null,
        ) : Condition

        @Serializable
        public data class SelectedPackage(
            public val operator: ArrayOperator,
            public val packages: List<String>,
        ) : Condition

        @Serializable
        public data class Variable(
            public val operator: EqualityOperator,
            public val variable: String,
            public val value: ConditionValue,
        ) : Condition

        @Serializable
        public object Unsupported : Condition
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object ConditionValueSerializer : KSerializer<ComponentOverride.ConditionValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ConditionValue")

    override fun deserialize(decoder: Decoder): ComponentOverride.ConditionValue {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Can only deserialize ConditionValue from JSON")
        val primitive = jsonDecoder.decodeJsonElement().jsonPrimitive
        return when {
            primitive.isString -> ComponentOverride.ConditionValue.StringValue(primitive.content)
            primitive.booleanOrNull != null ->
                ComponentOverride.ConditionValue.BoolValue(primitive.booleanOrNull!!)
            primitive.intOrNull != null ->
                ComponentOverride.ConditionValue.IntValue(primitive.intOrNull!!)
            primitive.doubleOrNull != null ->
                ComponentOverride.ConditionValue.DoubleValue(primitive.doubleOrNull!!)
            else -> throw SerializationException("Unexpected ConditionValue: $primitive")
        }
    }

    override fun serialize(encoder: Encoder, value: ComponentOverride.ConditionValue) {
        throw NotImplementedError("Serialization is not implemented because it is not needed.")
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object ConditionSerializer : SealedDeserializerWithDefault<Condition>(
    serialName = "Condition",
    serializerByType = mapOf(
        "compact" to { Condition.Compact.serializer() },
        "medium" to { Condition.Medium.serializer() },
        "expanded" to { Condition.Expanded.serializer() },
        "intro_offer" to { Condition.IntroOffer.serializer() },
        "multiple_intro_offers" to { Condition.MultiplePhaseOffers.serializer() },
        "selected" to { Condition.Selected.serializer() },
        "promo_offer" to { Condition.PromoOffer.serializer() },
        "selected_package" to { Condition.SelectedPackage.serializer() },
        "variable" to { Condition.Variable.serializer() },
    ),
    defaultValue = { Condition.Unsupported },
)
