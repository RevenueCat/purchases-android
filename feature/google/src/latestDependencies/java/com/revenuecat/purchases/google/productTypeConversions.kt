import com.android.billingclient.api.BillingClient
import com.revenuecat.purchases.ProductType

fun String?.toProductType(): ProductType {
    return when (this) {
        BillingClient.SkuType.INAPP -> ProductType.INAPP
        BillingClient.SkuType.SUBS -> ProductType.SUBS
        else -> ProductType.UNKNOWN
    }
}

fun ProductType.toSKUType(): String? {
    return when (this) {
        ProductType.INAPP -> BillingClient.SkuType.INAPP
        ProductType.SUBS -> BillingClient.SkuType.SUBS
        else -> null
    }
}
