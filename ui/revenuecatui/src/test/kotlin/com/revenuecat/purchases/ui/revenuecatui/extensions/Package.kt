package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.StoreProduct
import java.net.URL

internal fun Package.copy(
    identifier: String = this.identifier,
    packageType: PackageType = this.packageType,
    product: StoreProduct = this.product,
    presentedOfferingContext: PresentedOfferingContext = this.presentedOfferingContext,
    webCheckoutURL: URL? = this.webCheckoutURL,
): Package = Package(
    identifier = identifier,
    packageType = packageType,
    product = product,
    presentedOfferingContext = presentedOfferingContext,
    webCheckoutURL = webCheckoutURL,
)
