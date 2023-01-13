## v6 API Changes

### Classes

| New                    |
|------------------------|
| `PurchaseOption`       |
| `GooglePurchaseOption` |
| `GoogleStoreProduct`   |
| `AmazonStoreProduct`   |
| `Price`                |
| `PricingPhase`         |
| `RecurrenceMode`       |

| Old Location                            | New Location                                   |
|-----------------------------------------|------------------------------------------------|
| com.revenuecat.purchases.BillingFeature | com.revenuecat.purchases.models.BillingFeature |

### StoreProduct

StoreProduct has been made an interface, which `GoogleStoreProduct` and `AmazonStoreProduct` implement.

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

| Removed                           |  | 
|-----------------------------------|--|
| product.originalPrice             | Accessible in basePlan's unique pricing phase `purchaseOptions.firstOrNull{ it.isBasePlan }?.pricingPhases?.first()?.formattedPrice` |
| product.originalPriceAmountMicros | Accessible in basePlan's unique pricing phase `purchaseOptions.firstOrNull{ it.isBasePlan }?.pricingPhases?.first()?.priceAmountMicros` |
| product.iconUrl                  |  | 
| product.originalJson              |(product as GoogleStoreProduct).productDetails |

### StoreTransaction

| New              |
|------------------|
| purchaseOptionId |

| Deprecated | New        |
|------------|------------|
| skus       | productIds |

### UpgradeInfo updates


| Removed | New          |
|---------|--------------|
| oldSku  | oldProductId |


### CustomerInfo updates

| Deprecated              | New                           |
|-------------------------|-------------------------------|
| allPurchasedSkus        | allPurchasedProductIds        |
| getExpirationDateForSku | getExpirationDateForProductId |
| getPurchaseDateForSku   | getPurchaseDateForProductId   |

### Purchasing APIs

| New                                                                                                      |
|----------------------------------------------------------------------------------------------------------|
| `purchaseSubscriptionOption(Activity, StoreProduct, PurchaseOption, PurchaseCallback)`                   |
| `purchaseSubscriptionOption(Activity, StoreProduct, PurchaseOption, UpgradeInfo, ProductChangeCallback)` |

| Deprecated                                                       | New                                                   |
|------------------------------------------------------------------|-------------------------------------------------------|
| `getSubscriptionSkus(List<String>, GetStoreProductsCallback)`    | `getProducts(List<String>, GetStoreProductsCallback)` |
| `getNonSubscriptionSkus(List<String>, GetStoreProductsCallback)` | `getProducts(List<String>, GetStoreProductsCallback)` |

### Kotlin Helpers

| New                                                                                                                                                                |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `purchaseSubscriptionOptionWith(Activity, StoreProduct, PurchaseOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`              |
| `purchaseSubscriptionOptionWith(Activity, StoreProduct, UpgradeInfo, PurchaseOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |

| Deprecated                                                                                         | New                                                                                     |
|----------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| `getSubscriptionSkusWith(List<String>, (PurchasesError) -> Unit, (List<StoreProduct>) -> Unit)`    | `getProductsWith(List<String>, (PurchasesError) -> Unit, (List<StoreProduct>) -> Unit)` |
| `getNonSubscriptionSkusWith(List<String>, (PurchasesError) -> Unit, (List<StoreProduct>) -> Unit)` | `getProductsWith(List<String>, (PurchasesError) -> Unit, (List<StoreProduct>) -> Unit)` |

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
| `purchasePackageWith(Activity, Package, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)`                 |
| `purchasePackageWith(Activity, Package, UpgradeInfo, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)`    |
| `purchaseProductWith(Activity, SkuDetails, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)`              |
| `purchaseProductWith(Activity, SkuDetails, UpgradeInfo, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)` |

| Removed Listeners              |  
|--------------------------------|
| `ReceiveOfferingsListener`     |
| `GetSkusResponseListener`      |
| `ProductChangeListener`        |
| `MakePurchaseListener`         |
| `ReceivePurchaserInfoListener` |
| `UpdatedPurchaserInfoListener` |
| `PurchaseErrorListener`        |

| Removed properties                        |
|-------------------------------------------|
| CustomerInfo.purchasedNonSubscriptionSkus |
| CustomerInfo.jsonObject                   |
| EntitlementInfo.jsonObject                |

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
