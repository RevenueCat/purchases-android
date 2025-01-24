package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.utils.mapNotNullKeys
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * @property value The language tag of this locale, with an underscore separating the language from the region.
 */
@InternalRevenueCatAPI
@Serializable
@JvmInline
value class LocaleId(@get:JvmSynthetic val value: String)

@InternalRevenueCatAPI
@Serializable
@JvmInline
value class LocalizationKey(@get:JvmSynthetic val value: String)

/**
 * A make-shift union type. LocalizationData is either a plain String or a ThemeImageUrls object.
 */
@InternalRevenueCatAPI
@Serializable(with = LocalizationDataSerializer::class)
sealed interface LocalizationData {
    @Serializable
    @JvmInline
    value class Text(@get:JvmSynthetic val value: String) : LocalizationData

    @Serializable
    @JvmInline
    value class Image(@get:JvmSynthetic val value: ThemeImageUrls) : LocalizationData
}

@OptIn(InternalRevenueCatAPI::class)
private object LocalizationDataSerializer : KSerializer<LocalizationData> {
    // Documentation says to use either PrimitiveSerialDescriptor or buildClassSerialDescriptor. However, we need a
    // polymorphic descriptor that's either a primitive (string) or a class. So neither of those options fit the bill.
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        serialName = "LocalizationData",
        kind = PolymorphicKind.SEALED,
    )

    override fun serialize(encoder: Encoder, value: LocalizationData) {
        error("Serialization is not implemented as it is not (yet) needed.")
    }

    @Suppress("SwallowedException")
    override fun deserialize(decoder: Decoder): LocalizationData =
        // We have no `type` descriptor field, so we resort to trial and error.
        try {
            decoder.decodeSerializableValue(LocalizationData.Text.serializer())
        } catch (e: SerializationException) {
            decoder.decodeSerializableValue(LocalizationData.Image.serializer())
        }
}

/**
 * Deserializes a map of [LocaleId] to [VariableLocalizationKey] maps. The [VariableLocalizationKey] maps ignore unknown
 * [VariableLocalizationKey]s.
 */
@Suppress("MaxLineLength")
@InternalRevenueCatAPI
internal object LocalizedVariableLocalizationKeyMapSerializer : KSerializer<Map<LocaleId, Map<VariableLocalizationKey, String>>> {
    private val delegate = MapSerializer(
        keySerializer = LocaleId.serializer(),
        valueSerializer = VariableLocalizationKeyMapSerializer,
    )
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Map<LocaleId, Map<VariableLocalizationKey, String>>) {
        // Serialization is not implemented as it is not needed.
    }

    override fun deserialize(decoder: Decoder): Map<LocaleId, Map<VariableLocalizationKey, String>> =
        delegate.deserialize(decoder)
}

/**
 * Deserializes a map of [VariableLocalizationKey]s to String, ignoring unknown [VariableLocalizationKey]s.
 */
@InternalRevenueCatAPI
internal object VariableLocalizationKeyMapSerializer : KSerializer<Map<VariableLocalizationKey, String>> {
    private val delegate = MapSerializer(String.serializer(), String.serializer())

    // We can use mapSerialDescriptor<VariableLocalizationKey, String>() when that is no longer experimental. For now
    // using the delegate's descriptor is good enough, even though that has String keys.
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Map<VariableLocalizationKey, String>) {
        // Serialization is not implemented as it is not needed.
    }

    override fun deserialize(decoder: Decoder): Map<VariableLocalizationKey, String> =
        decoder.decodeSerializableValue(delegate).mapNotNullKeys { (stringKey, _) ->
            @Suppress("SwallowedException")
            try {
                VariableLocalizationKey.valueOf(stringKey.uppercase())
            } catch (e: IllegalArgumentException) {
                // Ignoring unknown VariableLocalizationKey.
                null
            }
        }
}

/**
 * Keys for localized strings used as variable values.
 */
@InternalRevenueCatAPI
@Serializable
enum class VariableLocalizationKey {
    @SerialName("day")
    DAY,

    @SerialName("daily")
    DAILY,

    @SerialName("day_short")
    DAY_SHORT,

    @SerialName("week")
    WEEK,

    @SerialName("weekly")
    WEEKLY,

    @SerialName("week_short")
    WEEK_SHORT,

    @SerialName("month")
    MONTH,

    @SerialName("monthly")
    MONTHLY,

    @SerialName("month_short")
    MONTH_SHORT,

    @SerialName("year")
    YEAR,

    @SerialName("yearly")
    YEARLY,

    @SerialName("year_short")
    YEAR_SHORT,

    @SerialName("annual")
    ANNUAL,

    @SerialName("annually")
    ANNUALLY,

    @SerialName("annual_short")
    ANNUAL_SHORT,

    @SerialName("free_price")
    FREE_PRICE,

    @SerialName("percent")
    PERCENT,

    @SerialName("num_day_zero")
    NUM_DAY_ZERO,

    @SerialName("num_day_one")
    NUM_DAY_ONE,

    @SerialName("num_day_two")
    NUM_DAY_TWO,

    @SerialName("num_day_few")
    NUM_DAY_FEW,

    @SerialName("num_day_many")
    NUM_DAY_MANY,

    @SerialName("num_day_other")
    NUM_DAY_OTHER,

    @SerialName("num_week_zero")
    NUM_WEEK_ZERO,

    @SerialName("num_week_one")
    NUM_WEEK_ONE,

    @SerialName("num_week_two")
    NUM_WEEK_TWO,

    @SerialName("num_week_few")
    NUM_WEEK_FEW,

    @SerialName("num_week_many")
    NUM_WEEK_MANY,

    @SerialName("num_week_other")
    NUM_WEEK_OTHER,

    @SerialName("num_month_zero")
    NUM_MONTH_ZERO,

    @SerialName("num_month_one")
    NUM_MONTH_ONE,

    @SerialName("num_month_two")
    NUM_MONTH_TWO,

    @SerialName("num_month_few")
    NUM_MONTH_FEW,

    @SerialName("num_month_many")
    NUM_MONTH_MANY,

    @SerialName("num_month_other")
    NUM_MONTH_OTHER,

    @SerialName("num_year_zero")
    NUM_YEAR_ZERO,

    @SerialName("num_year_one")
    NUM_YEAR_ONE,

    @SerialName("num_year_two")
    NUM_YEAR_TWO,

    @SerialName("num_year_few")
    NUM_YEAR_FEW,

    @SerialName("num_year_many")
    NUM_YEAR_MANY,

    @SerialName("num_year_other")
    NUM_YEAR_OTHER,
}
