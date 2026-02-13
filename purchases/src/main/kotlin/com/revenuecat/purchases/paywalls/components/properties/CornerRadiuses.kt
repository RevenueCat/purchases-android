package com.revenuecat.purchases.paywalls.components.properties

import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@InternalRevenueCatAPI
@Stable
@Serializable(with = CornerRadiusesSerializer::class)
public sealed interface CornerRadiuses {

    /**
     * Contains radius values for 4 corners, in dp.
     */
    @Poko
    @Immutable
    @Serializable
    public class Dp(
        /**
         * The top-leading, or top-start, corner radius, in dp.
         */
        @get:JvmSynthetic
        @SerialName("top_leading")
        public val topLeading: Double,
        /**
         * The top-trailing, or top-end, corner radius, in dp.
         */
        @get:JvmSynthetic
        @SerialName("top_trailing")
        public val topTrailing: Double,
        /**
         * The bottom-leading, or bottom-start, corner radius, in dp.
         */
        @get:JvmSynthetic
        @SerialName("bottom_leading")
        public val bottomLeading: Double,
        /**
         * The bottom-trailing, or bottom-end, corner radius, in dp.
         */
        @get:JvmSynthetic
        @SerialName("bottom_trailing")
        public val bottomTrailing: Double,
    ) : CornerRadiuses {
        public companion object {
            @get:JvmSynthetic public val zero: Dp = Dp(0.0, 0.0, 0.0, 0.0)

            @get:JvmSynthetic public val default: Dp = zero
        }

        public constructor(all: Double) : this(all, all, all, all)

        public fun copy(
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
    @Immutable
    @Serializable
    public class Percentage(
        /**
         * The top-leading, or top-start, corner radius, in percentage [0-100].
         */
        @get:JvmSynthetic
        @IntRange(from = 0, to = 100)
        @SerialName("top_leading")
        public val topLeading: Int,
        /**
         * The top-trailing, or top-end, corner radius, in percentage [0-100].
         */
        @get:JvmSynthetic
        @IntRange(from = 0, to = 100)
        @SerialName("top_trailing")
        public val topTrailing: Int,
        /**
         * The bottom-leading, or bottom-start, corner radius, in percentage [0-100].
         */
        @get:JvmSynthetic
        @IntRange(from = 0, to = 100)
        @SerialName("bottom_leading")
        public val bottomLeading: Int,
        /**
         * The bottom-trailing, or bottom-end, corner radius, in percentage [0-100].
         */
        @get:JvmSynthetic
        @IntRange(from = 0, to = 100)
        @SerialName("bottom_trailing")
        public val bottomTrailing: Int,
    ) : CornerRadiuses {

        public constructor(all: Int) : this(all, all, all, all)
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
        return decoder.decodeSerializableValue(serializer)
    }
}
