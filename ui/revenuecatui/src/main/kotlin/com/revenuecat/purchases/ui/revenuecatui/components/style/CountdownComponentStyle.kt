package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.properties.Size
import dev.drewhamilton.poko.Poko
import java.util.Date

@Poko
@Immutable
internal class CountdownComponentStyle(
    @get:JvmSynthetic
    val date: Date,
    @get:JvmSynthetic
    val countFrom: CountdownComponent.CountFrom,
    @get:JvmSynthetic
    val countdownStackComponentStyle: StackComponentStyle,
    @get:JvmSynthetic
    val endStackComponentStyle: StackComponentStyle?,
    @get:JvmSynthetic
    val fallbackStackComponentStyle: StackComponentStyle?,
) : ComponentStyle {
    override val visible: Boolean = countdownStackComponentStyle.visible
    override val size: Size = countdownStackComponentStyle.size
}
