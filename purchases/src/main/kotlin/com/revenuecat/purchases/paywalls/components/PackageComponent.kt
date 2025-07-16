package com.revenuecat.purchases.paywalls.components

import com.revenuecat.purchases.InternalRevenueCatAPI
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("package")
public class PackageComponent(
    @get:JvmSynthetic
    @SerialName("package_id")
    public val packageId: String,
    @get:JvmSynthetic
    @SerialName("is_selected_by_default")
    public val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    public val stack: StackComponent,
) : PaywallComponent
