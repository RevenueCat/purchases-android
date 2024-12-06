package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("package")
class PackageComponent internal constructor(
    @get:JvmSynthetic
    @SerialName("package_id")
    val packageId: String,
    @get:JvmSynthetic
    @SerialName("is_selected_by_default")
    val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    val stack: StackComponent,
) : PaywallComponent
