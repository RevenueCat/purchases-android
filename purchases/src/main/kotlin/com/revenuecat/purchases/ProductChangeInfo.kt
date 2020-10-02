package com.revenuecat.purchases

import com.android.billingclient.api.BillingFlowParams
import com.revenuecat.purchases.common.ProductInfo

/**
 * This object holds the information used when upgrading or downgrading from another product.
 * @property oldProduct The product to change from.
 * @property prorationMode The [BillingFlowParams.ProrationMode] to use when upgrading the given oldSku.
 */
data class ProductChangeInfo(
    val oldProduct: ProductInfo,
    @BillingFlowParams.ProrationMode val prorationMode: Int? = null
)
