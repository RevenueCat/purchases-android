@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.stack

import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit

internal val SizeConstraint.allowsFlexDistribution: Boolean
    get() = this !is Fit || (min ?: 0u) > 0u
