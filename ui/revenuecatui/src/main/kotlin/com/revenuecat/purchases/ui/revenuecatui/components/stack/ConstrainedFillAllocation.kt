@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import kotlin.math.roundToInt
import kotlin.math.sign

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
    var remainingSpace = availableSpace.coerceAtLeast(0).toLong()

    while (remainingIndices.isNotEmpty()) {
        val equalShare = remainingSpace.toDouble() / remainingIndices.size
        val minimumConstrainedIndices = remainingIndices.filter { index ->
            val fill = requireNotNull(constraints[index])
            equalShare < fill.minimumPx(density)
        }
        val constrainedIndices = if (minimumConstrainedIndices.isNotEmpty()) {
            minimumConstrainedIndices
        } else {
            remainingIndices.filter { index ->
                equalShare > requireNotNull(constraints[index]).maximumPx(density)
            }
        }

        if (constrainedIndices.isEmpty()) {
            // Match Row/Column's weight rounding: round each equal share, then correct the rounding error one
            // pixel at a time starting from the first child.
            val roundedShare = equalShare.roundToInt()
            var remainder = remainingSpace.toInt() - roundedShare * remainingIndices.size
            remainingIndices.forEach { index ->
                val correction = remainder.sign
                result[index] = roundedShare + correction
                remainder -= correction
            }
            break
        }

        constrainedIndices.forEach { index ->
            val fill = requireNotNull(constraints[index])
            val allocation = if (minimumConstrainedIndices.isNotEmpty()) {
                fill.minimumPx(density)
            } else {
                fill.maximumPx(density)
            }
            result[index] = allocation
            remainingSpace -= allocation.toLong()
            remainingIndices.remove(index)
        }
        remainingSpace = remainingSpace.coerceAtLeast(0L)
    }

    return result
}

private fun Fill.minimumPx(density: Density): Int = min?.toPx(density) ?: 0

private fun Fill.maximumPx(density: Density): Int =
    maxOf(max?.toPx(density) ?: Constraints.Infinity, minimumPx(density))

private fun UInt.toPx(density: Density): Int =
    (toDouble() * density.density).roundToInt().coerceAtLeast(0)
