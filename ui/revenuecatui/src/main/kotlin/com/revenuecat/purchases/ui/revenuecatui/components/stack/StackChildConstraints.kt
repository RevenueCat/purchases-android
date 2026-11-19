@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import kotlin.math.roundToInt

/** Keep available scroll-viewport minimums without imposing them on Fit/Fixed children or lifting a Fill maximum. */
internal fun Constraints.preserveUnboundedFillMinimums(
    size: Size?,
    parent: Constraints,
    density: Density,
): Constraints =
    copy(
        minWidth = if (!hasBoundedWidth) {
            size?.width.fillMinimum(parent.minWidth, minWidth, density)
        } else {
            minWidth
        },
        minHeight = if (!hasBoundedHeight) {
            size?.height.fillMinimum(parent.minHeight, minHeight, density)
        } else {
            minHeight
        },
    )

private fun SizeConstraint?.fillMinimum(available: Int, current: Int, density: Density): Int {
    if (this !is Fill) return current
    val maximum = max?.let { (maxOf(it, min ?: 0u).toDouble() * density.density).roundToInt() }
    return maxOf(current, minOf(available, maximum ?: available))
}
