@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint

/**
 * Orientation-relative accessors so [ConstrainedFillLayout] can be written once for rows and columns.
 */

internal fun Size.mainAxisConstraint(orientation: Orientation): SizeConstraint =
    if (orientation == Orientation.Horizontal) width else height

internal fun Size.crossAxisConstraint(orientation: Orientation): SizeConstraint =
    if (orientation == Orientation.Horizontal) height else width

internal fun Placeable.mainAxisSize(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) width else height

internal fun Placeable.crossAxisSize(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) height else width

internal fun Constraints.mainAxisMin(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) minWidth else minHeight

internal fun Constraints.mainAxisMax(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) maxWidth else maxHeight

internal fun Constraints.crossAxisMin(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) minHeight else minWidth

internal fun Constraints.crossAxisMax(orientation: Orientation): Int =
    if (orientation == Orientation.Horizontal) maxHeight else maxWidth

internal fun Constraints.isFullyUnbounded(orientation: Orientation): Boolean =
    mainAxisMax(orientation) == Constraints.Infinity && mainAxisMin(orientation) == 0
