package com.revenuecat.purchases.ui.revenuecatui

import com.revenuecat.purchases.models.GoogleReplacementMode

/**
 * Data class used to start the correct product upgrade flow for paywalls.
 * @param oldProductId Product ID (i.e. sku) for the previous subscription
 * @param googleReplacementMode [GoogleReplacementMode] mode to be used for the upgrade flow
 */
data class ReplaceProductData(val oldProductId: String, val googleReplacementMode: GoogleReplacementMode)
