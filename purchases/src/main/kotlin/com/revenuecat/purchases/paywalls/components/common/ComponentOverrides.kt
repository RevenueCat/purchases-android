package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.PartialComponent
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
class ComponentOverrides<T : PartialComponent>(
    @get:JvmSynthetic val introOffer: T? = null,
    @get:JvmSynthetic val states: ComponentStates<T>? = null,
    @get:JvmSynthetic val conditions: ComponentConditions<T>? = null,
)

@InternalRevenueCatAPI
@Poko
@Serializable
class ComponentStates<T : PartialComponent>(
    @get:JvmSynthetic val selected: T? = null,
)

@InternalRevenueCatAPI
@Poko
@Serializable
class ComponentConditions<T : PartialComponent>(
    @get:JvmSynthetic val compact: T? = null,
    @get:JvmSynthetic val medium: T? = null,
    @get:JvmSynthetic val expanded: T? = null,
)
