package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.PaywallData.LocalizedConfiguration.OfferOverride

@Suppress("LongParameterList")
internal fun PaywallData.LocalizedConfiguration.copy(
    title: String = this.title,
    subtitle: String? = this.subtitle,
    callToAction: String = this.callToAction,
    callToActionWithIntroOffer: String? = this.callToActionWithIntroOffer,
    callToActionWithMultipleIntroOffers: String? = this.callToActionWithMultipleIntroOffers,
    offerDetails: String? = this.offerDetails,
    offerDetailsWithIntroOffer: String? = this.offerDetailsWithIntroOffer,
    offerDetailsWithMultipleIntroOffers: String? = this.offerDetailsWithMultipleIntroOffers,
    offerName: String? = this.offerName,
    features: List<PaywallData.LocalizedConfiguration.Feature> = this.features,
    tierName: String? = this.tierName,
    offerOverrides: Map<String, OfferOverride> = this.offerOverrides,
): PaywallData.LocalizedConfiguration = PaywallData.LocalizedConfiguration(
    title = title,
    subtitle = subtitle,
    callToAction = callToAction,
    callToActionWithIntroOffer = callToActionWithIntroOffer,
    callToActionWithMultipleIntroOffers = callToActionWithMultipleIntroOffers,
    offerDetails = offerDetails,
    offerDetailsWithIntroOffer = offerDetailsWithIntroOffer,
    offerDetailsWithMultipleIntroOffers = offerDetailsWithMultipleIntroOffers,
    offerName = offerName,
    features = features,
    tierName = tierName,
    offerOverrides = offerOverrides
)
