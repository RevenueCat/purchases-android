@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit

internal val SizeConstraint.hasPositiveFitMinimum: Boolean
    get() = this is Fit && (min ?: 0u) > 0u

internal val SizeConstraint.allowsFlexDistribution: Boolean
    get() = this !is Fit || hasPositiveFitMinimum

internal fun SizeConstraint.requiresFitMinimumLayout(distribution: FlexDistribution): Boolean =
    hasPositiveFitMinimum && distribution.usesAllAvailableSpace

internal fun SizeConstraint.shouldFitMainAxis(hasFillChildren: Boolean): Boolean =
    this is Fit && (!hasFillChildren || hasPositiveFitMinimum)
