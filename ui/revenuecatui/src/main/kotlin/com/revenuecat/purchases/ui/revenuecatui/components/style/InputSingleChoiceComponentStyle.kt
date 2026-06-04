package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.paywalls.components.properties.Size

@Immutable
internal data class InputSingleChoiceComponentStyle(
    @get:JvmSynthetic
    val fieldId: String,
    @get:JvmSynthetic
    val stack: StackComponentStyle,
) : ComponentStyle {
    override val visible: Boolean = stack.visible
    override val size: Size = stack.size
}

@Immutable
internal data class InputOptionComponentStyle(
    @get:JvmSynthetic
    val fieldId: String,
    @get:JvmSynthetic
    val optionId: String,
    @get:JvmSynthetic
    val stack: StackComponentStyle,
) : ComponentStyle {
    override val visible: Boolean = stack.visible
    override val size: Size = stack.size
}
