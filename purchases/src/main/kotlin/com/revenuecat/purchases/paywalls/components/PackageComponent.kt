package com.revenuecat.purchases.paywalls.components

import androidx.compose.runtime.Immutable
import com.revenuecat.purchases.InternalRevenueCatAPI
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
    val packageId: String,
    @get:JvmSynthetic
    @SerialName("is_selected_by_default")
    val isSelectedByDefault: Boolean,
    @get:JvmSynthetic
    val stack: StackComponent,
    @get:JvmSynthetic
    @Serializable(with = ResilientPromoOfferConfigSerializer::class)
    @SerialName("play_store_offer")
    val playStoreOffer: PromoOfferConfig? = null,
) : PaywallComponent
