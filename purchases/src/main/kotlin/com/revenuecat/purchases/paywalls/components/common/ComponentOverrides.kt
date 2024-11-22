package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.paywalls.components.PartialComponent
import kotlinx.serialization.Serializable

@Serializable
internal data class ComponentOverrides<T : PartialComponent>(
    val introOffer: T? = null,
    val states: ComponentStates<T>? = null,
    val conditions: ComponentConditions<T>? = null,
)

@Serializable
internal data class ComponentStates<T : PartialComponent>(
    val selected: T? = null,
)

@Serializable
internal data class ComponentConditions<T : PartialComponent>(
    val compact: T? = null,
    val medium: T? = null,
    val expanded: T? = null,
)
