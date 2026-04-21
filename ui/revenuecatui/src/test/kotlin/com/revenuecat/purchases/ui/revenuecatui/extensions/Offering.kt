package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import java.net.URL

@Suppress("LongParameterList")
internal fun Offering.copy(
    identifier: String = this.identifier,
    serverDescription: String = this.serverDescription,
    metadata: Map<String, Any> = this.metadata,
    availablePackages: List<Package> = this.availablePackages,
    paywall: PaywallData? = this.paywall,
    paywallComponents: Offering.PaywallComponents? = this.paywallComponents,
    webCheckoutURL: URL? = this.webCheckoutURL,
): Offering = Offering(
    identifier = identifier,
    serverDescription = serverDescription,
    metadata = metadata,
    availablePackages = availablePackages,
    paywall = paywall,
    paywallComponents = paywallComponents,
    webCheckoutURL = webCheckoutURL,
)
