package com.revenuecat.purchases.google

import com.android.billingclient.api.QueryProductDetailsParams

internal val QueryProductDetailsParams.Product.productId
    get() = this.zza()
internal val QueryProductDetailsParams.Product.productType
    get() = this.zzb()
@Suppress("UNCHECKED_CAST")
internal val QueryProductDetailsParams.productList: List<QueryProductDetailsParams.Product>
    get() = this.zza() as List<QueryProductDetailsParams.Product>
internal val QueryProductDetailsParams.productType
    get() = this.zzb()
