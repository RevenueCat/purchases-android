package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
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
    @get:JvmSynthetic public val style: CountdownStyle,
    @get:JvmSynthetic public val visible: Boolean? = null,
    @SerialName("count_from")
    @get:JvmSynthetic public val countFrom: CountFrom = CountFrom.DAYS,
    @SerialName("countdown_stack")
    @get:JvmSynthetic public val countdownStack: StackComponent,
    @SerialName("end_stack")
    @get:JvmSynthetic public val endStack: StackComponent? = null,
    @get:JvmSynthetic public val fallback: StackComponent? = null,
    @get:JvmSynthetic public val overrides: List<ComponentOverride<PartialCountdownComponent>> = emptyList(),
) : PaywallComponent {

    @InternalRevenueCatAPI
    @Poko
    @Serializable
    @Immutable
    public class CountdownStyle(
        @get:JvmSynthetic public val type: String,
        @Serializable(with = ISO8601DateSerializer::class)
        @get:JvmSynthetic public val date: Date,
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

/**
 * The subset of [CountdownComponent] properties that can be adjusted by a rule-based
 * [ComponentOverride]. Only [visible] is overridable; all other properties always come from the
 * base component.
 */
@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialCountdownComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = true,
) : PartialComponent
