@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.Size

internal fun Modifier.resolveComponentSizeParentData(size: Size): Modifier =
    if (any { it is ComponentSizeParentDataModifier }) {
        ComponentSizeParentDataModifier(size).then(this)
    } else {
        this
    }

internal data class ComponentSizeParentDataModifier(
    val size: Size,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@ComponentSizeParentDataModifier
}
