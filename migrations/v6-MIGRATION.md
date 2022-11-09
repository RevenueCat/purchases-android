## v6 API changes

### New

| Name                   |
|------------------------|
| `PurchaseOption`       |
| `GooglePurchaseOption` |
| `GoogleStoreProduct`   |
| `Price`                |
| `PricingPhase`         |
| `RecurrenceMode`       |

### StoreProduct updates

| Previous                      | New                                                                           |
|-------------------------------|-------------------------------------------------------------------------------|
| price                         | Moved to PricingPhase                                                         |
| priceAmountMicros             | Moved to PricingPhase                                                         |
| priceCurrencyCode             | Moved to PricingPhase                                                         |
| freeTrialPeriod               | Moved to PricingPhase                                                         |
| introductoryPrice             | Moved to PricingPhase                                                         |
| introductoryPriceAmountMicros | Moved to PricingPhase                                                         |
| introductoryPricePeriod       | Moved to PricingPhase                                                         |
| introductoryPriceCycles       | Moved to PricingPhase                                                         |
| originalJson                  | `productDetails` in GoogleStoreProduct; `amazonProduct` in AmazonStoreProduct |

| New                 |
|---------------------|
| oneTimeProductPrice |
| purchaseOptions     |

| Removed                   |
|---------------------------|
| originalPrice             |
| originalPriceAmountMicros |
| iconUrl                   |
| originalJson              |

### StoreTransaction updates

| Deprecated | New        |
|------------|------------|
| skus       | productIds |

### Type Changes

| Old type name | New type name  |
|---------------|----------------|

### Deprecated + New APIs

| Deprecated                                                      | New                                 |
|-----------------------------------------------------------------|-------------------------------------|

### Kotlin Helpers Changes

| Old signature | New signature |
|---------------|---------------|

| Removed|  
|----------------------------------------------------------------------------------------------------------------------|
| `getPurchaserInfoWith((PurchasesError) -> Unit, (PurchaserInfo) -> Unit)`                                            |
| `purchasePackageWith(Activity, Package, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)`                |
| `purchasePackageWith(Activity, Package, UpgradeInfo, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)`   |
| `purchaseProductWith(Activity, SkuDetails, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)`             |
| `purchaseProductWith(Activity, SkuDetails, UpgradeInfo, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)` |

### Moved

| Old location                            | New location                                   |
|-----------------------------------------|------------------------------------------------|
| com.revenuecat.purchases.BillingFeature | com.revenuecat.purchases.models.BillingFeature |

### Removed APIs

| Removed APIs                                                                                                          |  
|-----------------------------------------------------------------------------------------------------------------------|
| `configure(Context, String, String?, Boolean, ExecutorService)`                                                       |
| `getOfferings(ReceiveOfferingsListener)`                                                                              |
| `getSubscriptionSkus(List<String>, GetSkusResponseListener)`                                                          |
| `getNonSubscriptionSkus(List<String>, GetSkusResponseListener)`                                                       |
| `getNonSubscriptionSkus(skus, GetSkusResponseListener)`                                                               |
| `purchaseProduct(Activity, SkuDetails, MakePurchaseListener)`                                                         |
| `purchaseProduct(Activity, StoreProduct, MakePurchaseListener)`                                                       |
| `purchaseProduct(Activity, SkuDetails, UpgradeInfo, ProductChangeListener)`                                           |
| `purchasePackage(Activity, Package, MakePurchaseListener)`                                                            |
| `purchasePackage(Activity, Package, UpgradeInfo, ProductChangeListener)`                                              |
| `restorePurchases(ReceivePurchaserInfoListener)`                                                                      |
| `logOut(ReceivePurchaserInfoListener)`                                                                                |
| `getPurchaserInfo(ReceivePurchaserInfoListener)`                                                                      |
| `getPurchaserInfo(ReceiveCustomerInfoCallback)`                                                                       |
| `getCustomerInfo(ReceivePurchaserInfoListener)`                                                                       |
| `invalidatePurchaserInfoCache()`                                                                                      |
| `removeUpdatedPurchaserInfoListener()`                                                                                |
| `setUpdatedPurchaserInfoListener(UpdatedPurchaserInfoListener)`                                                       |
| `getPurchaserInfoWith((PurchasesError) -> Unit, (PurchaserInfo) -> Unit)`                                             |
| `purchaseProductWith(Activity, SkuDetails, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)`              |
| `purchaseProductWith(Activity, SkuDetails, UpgradeInfo, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)` |

| Removed                        |  
|--------------------------------|
| `ReceiveOfferingsListener`     |
| `GetSkusResponseListener`      |
| `ProductChangeListener`        |
| `MakePurchaseListener`         |
| `ReceivePurchaserInfoListener` |
| `UpdatedPurchaserInfoListener` |
| `PurchaseErrorListener` |

| Removed properties                        |
|-------------------------------------------|
| CustomerInfo.purchasedNonSubscriptionSkus |
| CustomerInfo.jsonObject                   |
| EntitlementInfo.jsonObject                |

### Other changes:

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).


// TODO:
- listenerconversions
- Purchases
