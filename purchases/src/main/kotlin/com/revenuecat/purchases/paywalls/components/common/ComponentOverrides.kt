package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class ComponentOverrides<T : PartialComponent> internal constructor(
    val introOffer: T? = null,
    val states: ComponentStates<T>? = null,
    val conditions: ComponentConditions<T>? = null,
)

@InternalRevenueCatAPI
@Poko
@Serializable
class ComponentStates<T : PartialComponent> internal constructor(
    val selected: T? = null,
)

@InternalRevenueCatAPI
@Poko
@Serializable
class ComponentConditions<T : PartialComponent> internal constructor(
    val compact: T? = null,
    val medium: T? = null,
    val expanded: T? = null,
)
