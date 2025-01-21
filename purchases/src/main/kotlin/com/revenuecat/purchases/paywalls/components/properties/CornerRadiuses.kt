package com.revenuecat.purchases.paywalls.components.properties

import androidx.annotation.IntRange
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@InternalRevenueCatAPI
@Serializable(with = CornerRadiusesSerializer::class)
sealed interface CornerRadiuses {

    /**
     * Contains radius values for 4 corners, in dp.
     */
    @Poko
    @Serializable
    class Dp(
        /**
         * The top-leading, or top-start, corner radius, in dp.
         */
        @get:JvmSynthetic
        @SerialName("top_leading")
        val topLeading: Double,
        /**
         * The top-trailing, or top-end, corner radius, in dp.
         */
        @get:JvmSynthetic
        @SerialName("top_trailing")
        val topTrailing: Double,
        /**
         * The bottom-leading, or bottom-start, corner radius, in dp.
         */
        @get:JvmSynthetic
        @SerialName("bottom_leading")
        val bottomLeading: Double,
        /**
         * The bottom-trailing, or bottom-end, corner radius, in dp.
         */
        @get:JvmSynthetic
        @SerialName("bottom_trailing")
        val bottomTrailing: Double,
    ) : CornerRadiuses {
        companion object {
            @get:JvmSynthetic val zero = Dp(0.0, 0.0, 0.0, 0.0)

            @get:JvmSynthetic val default = zero
        }

        constructor(all: Double) : this(all, all, all, all)

        fun copy(
            topLeading: Double = this.topLeading,
            topTrailing: Double = this.topTrailing,
            bottomLeading: Double = this.bottomLeading,
            bottomTrailing: Double = this.bottomTrailing,
        ): Dp {
            return Dp(topLeading, topTrailing, bottomLeading, bottomTrailing)
        }
    }

    /**
     * Contains radius values for 4 corners, in percentages.
     */
    @Poko
    @Serializable
    class Percentage(
        /**
         * The top-leading, or top-start, corner radius, in percentage [0-100].
         */
        @get:JvmSynthetic
        @IntRange(from = 0, to = 100)
        @SerialName("top_leading")
        val topLeading: Int,
        /**
         * The top-trailing, or top-end, corner radius, in percentage [0-100].
         */
        @get:JvmSynthetic
        @IntRange(from = 0, to = 100)
        @SerialName("top_trailing")
        val topTrailing: Int,
        /**
         * The bottom-leading, or bottom-start, corner radius, in percentage [0-100].
         */
        @get:JvmSynthetic
        @IntRange(from = 0, to = 100)
        @SerialName("bottom_leading")
        val bottomLeading: Int,
        /**
         * The bottom-trailing, or bottom-end, corner radius, in percentage [0-100].
         */
        @get:JvmSynthetic
        @IntRange(from = 0, to = 100)
        @SerialName("bottom_trailing")
        val bottomTrailing: Int,
    ) : CornerRadiuses {

        constructor(all: Int) : this(all, all, all, all)
    }
}

@OptIn(InternalRevenueCatAPI::class)
internal object CornerRadiusesSerializer : KSerializer<CornerRadiuses> {
    private val serializer = CornerRadiuses.Dp.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: CornerRadiuses) {
        // We don't need to serialize CornerRadiuses.
    }

    /**
     * For now, the backend will always return corner radiuses in Dp, so this will assume all serializations
     * are done with that serializer.
     */
    override fun deserialize(decoder: Decoder): CornerRadiuses {
        return try {
            decoder.decodeSerializableValue(serializer)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            errorLog("Failed to deserialize CornerRadiuses", e)
            CornerRadiuses.Dp.zero
        }
    }
}
