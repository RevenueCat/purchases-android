@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import kotlin.math.roundToInt

/**
 * Divides [availableSpace] equally between Fill children while honoring their minimums and maximums.
 *
 * A child fixed at a limit leaves the remaining space for the other Fill children. If the minimums
 * cannot fit, they are preserved and the children overflow the parent sequentially rather than
 * drawing on top of each other.
 */
internal fun allocateConstrainedFillSpace(
    availableSpace: Int,
    constraints: List<Fill?>,
    density: Density,
): IntArray {
    val result = IntArray(constraints.size)
    val remainingIndices = constraints.indices.filterTo(mutableListOf()) { constraints[it] != null }
    var remainingSpace = availableSpace.coerceAtLeast(0)

    while (remainingIndices.isNotEmpty()) {
        val equalShare = remainingSpace.toDouble() / remainingIndices.size
        val constrainedIndices = remainingIndices.filter { index ->
            val fill = requireNotNull(constraints[index])
            equalShare < fill.minimumPx(density) || equalShare > fill.maximumPx(density)
        }

        if (constrainedIndices.isEmpty()) {
            val baseShare = remainingSpace / remainingIndices.size
            remainingIndices.forEachIndexed { remainderIndex, index ->
                result[index] = baseShare + if (remainderIndex < remainingSpace % remainingIndices.size) 1 else 0
            }
            break
        }

        constrainedIndices.forEach { index ->
            val fill = requireNotNull(constraints[index])
            val allocation = if (equalShare < fill.minimumPx(density)) {
                fill.minimumPx(density)
            } else {
                fill.maximumPx(density)
            }
            result[index] = allocation
            remainingSpace -= allocation
            remainingIndices.remove(index)
        }
        remainingSpace = remainingSpace.coerceAtLeast(0)
    }

    return result
}

private fun Fill.minimumPx(density: Density): Int = min?.toPx(density) ?: 0

private fun Fill.maximumPx(density: Density): Int =
    maxOf(max?.toPx(density) ?: Constraints.Infinity, minimumPx(density))

private fun UInt.toPx(density: Density): Int =
    (toDouble() * density.density).roundToInt().coerceAtLeast(0)
