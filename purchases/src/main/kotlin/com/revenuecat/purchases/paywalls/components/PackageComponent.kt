package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.common.PromoOfferConfig
import com.revenuecat.purchases.paywalls.components.common.ResilientPromoOfferConfigSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalRevenueCatAPI
@Poko
@Serializable
@SerialName("package")
@Immutable
public class PackageComponent(
    @get:JvmSynthetic
    @SerialName("package_id")
    public val packageId: String,
    @get:JvmSynthetic
    @SerialName("is_selected_by_default")
    public val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    public val stack: StackComponent,
    @get:JvmSynthetic
    @Serializable(with = ResilientPromoOfferConfigSerializer::class)
    @SerialName("play_store_offer")
    public val playStoreOffer: PromoOfferConfig? = null,
    @get:JvmSynthetic
    @SerialName("name")
    public val name: String? = null,
    @get:JvmSynthetic
    public val visible: Boolean? = null,
    @get:JvmSynthetic
    public val overrides: List<ComponentOverride<PartialPackageComponent>> = emptyList(),
) : PaywallComponent

@InternalRevenueCatAPI
@Poko
@Serializable
@Immutable
public class PartialPackageComponent(
    @get:JvmSynthetic
    public val visible: Boolean? = null,
) : PartialComponent
