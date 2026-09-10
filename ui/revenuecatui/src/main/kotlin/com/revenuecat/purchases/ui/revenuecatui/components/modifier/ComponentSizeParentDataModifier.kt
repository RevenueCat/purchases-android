@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.Size

/**
 * Replaces the [ComponentSizeParentDataModifier] a parent stack attached to this chain with one carrying the
 * component's fully resolved [size] (overrides and margin applied).
 *
 * Compose folds parent data from the innermost modifier outwards, so the outermost [ParentDataModifier] wins. The
 * resolved modifier is therefore prepended (`then(this)`) rather than appended. Does nothing when no parent asked for
 * size parent data.
 */
internal fun Modifier.resolveComponentSizeParentData(size: Size): Modifier =
    if (any { it is ComponentSizeParentDataModifier }) {
        ComponentSizeParentDataModifier(size).then(this)
    } else {
        this
    }

/**
 * Parent data read by `ConstrainedFillLayout` to allocate main-axis space to a child.
 *
 * [modifyParentData] intentionally ignores the incoming value: this must stay the outermost [ParentDataModifier] on
 * a component, and any other parent data (e.g. `Modifier.layoutId`) applied outside of it would hide it from the
 * stack, which would then treat the child as non-Fill.
 */
internal data class ComponentSizeParentDataModifier(
    val size: Size,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@ComponentSizeParentDataModifier
}
