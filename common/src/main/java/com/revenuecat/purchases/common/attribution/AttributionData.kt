package com.revenuecat.purchases.common.attribution

import org.json.JSONObject

data class AttributionData(
    val data: JSONObject,
    val network: AttributionNetwork,
    val networkUserId: String?
)
