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
@SerialName("countdown_dev")
@Immutable
class CountdownComponent(
    @get:JvmSynthetic val style: CountdownStyle,
    @SerialName("countdown_stack")
    @get:JvmSynthetic val countdownStack: StackComponent,
    @SerialName("end_stack")
    @get:JvmSynthetic val endStack: StackComponent? = null,
    @get:JvmSynthetic val fallback: StackComponent? = null,
) : PaywallComponent {

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    @Immutable
    class CountdownStyle(
        @get:JvmSynthetic val type: String,
        @Serializable(with = ISO8601DateSerializer::class)
        @get:JvmSynthetic val date: Date,
    )
}
