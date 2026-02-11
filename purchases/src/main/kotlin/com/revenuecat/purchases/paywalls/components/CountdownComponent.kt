package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.ISO8601DateSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("countdown")
@Immutable
public class CountdownComponent(
    public @get:JvmSynthetic val style: CountdownStyle,
    @SerialName("count_from")
    public @get:JvmSynthetic val countFrom: CountFrom = CountFrom.DAYS,
    @SerialName("countdown_stack")
    public @get:JvmSynthetic val countdownStack: StackComponent,
    @SerialName("end_stack")
    public @get:JvmSynthetic val endStack: StackComponent? = null,
    public @get:JvmSynthetic val fallback: StackComponent? = null,
) : PaywallComponent {

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    @Immutable
    public class CountdownStyle(
        public @get:JvmSynthetic val type: String,
        @Serializable(with = ISO8601DateSerializer::class)
        public @get:JvmSynthetic val date: Date,
    )

    @InternalRevenueCatAPI
    @Serializable
    public enum class CountFrom {
        @SerialName("days")
        DAYS,

        @SerialName("hours")
        HOURS,

        @SerialName("minutes")
        MINUTES,
    }
}
