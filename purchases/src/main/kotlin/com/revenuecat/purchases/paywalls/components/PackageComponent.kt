package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Serializable
@SerialName("package")
internal data class PackageComponent(
    @SerialName("package_id")
    val packageId: String,
    @SerialName("is_selected_by_default")
    val isSelectedByDefault: Boolean,
    val stack: StackComponent,
) : PaywallComponent
