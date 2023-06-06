package com.revenuecat.purchases.google

import com.android.billingclient.api.QueryProductDetailsParams

val QueryProductDetailsParams.Product.productId
    get() = this.zza()
val QueryProductDetailsParams.Product.productType
    get() = this.zzb()

@Suppress("UNCHECKED_CAST")
val QueryProductDetailsParams.productList: List<QueryProductDetailsParams.Product>
    get() = this.zza() as List<QueryProductDetailsParams.Product>
val QueryProductDetailsParams.productType
    get() = this.zzb()
