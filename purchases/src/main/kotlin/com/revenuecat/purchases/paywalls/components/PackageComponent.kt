package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
@SerialName("package")
internal data class PackageComponent(
    @get:JvmSynthetic
    @SerialName("package_id")
    val packageId: String,
    @get:JvmSynthetic
    @SerialName("is_selected_by_default")
    val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    val stack: StackComponent,
) : PaywallComponent
