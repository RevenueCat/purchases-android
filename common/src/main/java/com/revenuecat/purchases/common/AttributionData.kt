package com.revenuecat.purchases.common

import com.revenuecat.purchases.common.attribution.AttributionNetwork
import org.json.JSONObject

data class AttributionData(
    val data: JSONObject,
    val network: AttributionNetwork,
    val networkUserId: String?
)
