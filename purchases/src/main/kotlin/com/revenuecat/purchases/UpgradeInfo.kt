package com.revenuecat.purchases

data class UpgradeInfo(
        val oldSku: String,
        val prorationMode: Int? = null
)