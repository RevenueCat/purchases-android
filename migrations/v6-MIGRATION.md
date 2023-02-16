## v6 API Changes

### Classes

| New                    |
|------------------------|
| `SubscriptionOption`       |
| `GoogleSubscriptionOption` |
| `GoogleStoreProduct`   |
| `AmazonStoreProduct`   |
| `Price`                |
| `PricingPhase`         |
| `RecurrenceMode`       |
| `GoogleProrationMode`  |

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
| subscriptionOptions     |

| Removed                           |  | 
|-----------------------------------|--|
| product.originalPrice             | Accessible in basePlan's unique pricing phase `subscriptionOptions.firstOrNull{ it.isBasePlan }?.pricingPhases?.first()?.formattedPrice` |
| product.originalPriceAmountMicros | Accessible in basePlan's unique pricing phase `subscriptionOptions.firstOrNull{ it.isBasePlan }?.pricingPhases?.first()?.priceAmountMicros` |
| product.iconUrl                  |  | 
| product.originalJson              |(product as GoogleStoreProduct).productDetails |

### StoreTransaction

| New              |
|------------------|
| subscriptionOptionId |

| Deprecated | New        |
|------------|------------|
| skus       | productIds |

### UpgradeInfo updates


| Removed       | New                 |
|---------------|---------------------|
| oldSku        | oldProductId        |
| prorationMode | googleProrationMode |


### CustomerInfo updates

| Deprecated              | New                           |
|-------------------------|-------------------------------|
| allPurchasedSkus        | allPurchasedProductIds        |
| getExpirationDateForSku | getExpirationDateForProductId |
| getPurchaseDateForSku   | getPurchaseDateForProductId   |

### Purchasing APIs

The `purchasePackage()` and `purchaseProduct()` APIs have a new behavior for selecting which `SubscriptionOption` 
is used when purchasing a `Package` or `StoreProduct`. These functions use the following logic to choose
a [SubscriptionOption] to purchase:
*   - Filters out offers with "rc-ignore-default-offer" tag
*   - Uses [SubscriptionOption] WITH longest free trial or cheapest first phase
*   - Falls back to use base plan

For more control, the `purchaseSubscriptionOption()` API can be used to manually choose which option to purchase.

All purchasing functions now have a duplicate version including the `isPersonalizedPrice` Boolean parameter for 
compliance with EU regulations. See https://developer.android.com/google/play/billing/integrate#personalized-price 
for more info.

| New                                                                                                                 |
|---------------------------------------------------------------------------------------------------------------------|
| `purchaseSubscriptionOption(Activity, StoreProduct, SubscriptionOption, PurchaseCallback)`                          |
| `purchaseSubscriptionOption(Activity, StoreProduct, SubscriptionOption, Boolean, PurchaseCallback)`                 |
| `purchaseSubscriptionOption(Activity, StoreProduct, SubscriptionOption, UpgradeInfo, ProductChangeCallback)`        |
| `purchaseSubscriptionOption(Activity, StoreProduct, SubscriptionOption, UpgradeInfo, Boolean, ProductChangeCallback)` |
| `purchasePackage(Activity, Package, Boolean, PurchaseCallback)`                                                     |
| `purchaseProduct(Activity, StoreProduct, Boolean, PurchaseCallback)`                                |
| `purchasePackage(Activity, Package, Boolean, UpgradeInfo, ProductChangeCallback)`                                   |
| `purchaseProduct(Activity, StoreProduct, Boolean, UpgradeInfo, ProductChangeCallback)`                              |

| Deprecated                                                       | New                                                   |
|------------------------------------------------------------------|-------------------------------------------------------|
| `getSubscriptionSkus(List<String>, GetStoreProductsCallback)`    | `getProducts(List<String>, GetStoreProductsCallback)` |
| `getNonSubscriptionSkus(List<String>, GetStoreProductsCallback)` | `getProducts(List<String>, GetStoreProductsCallback)` |

### Kotlin Helpers

| New                                                                                                                                                                             |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `purchaseSubscriptionOptionWith(Activity, StoreProduct, SubscriptionOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`                       |
| `purchaseSubscriptionOptionWith(Activity, StoreProduct, SubscriptionOption, Boolean, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`              |
| `purchaseSubscriptionOptionWith(Activity, StoreProduct, UpgradeInfo, SubscriptionOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`          |
| `purchaseSubscriptionOptionWith(Activity, StoreProduct, UpgradeInfo, SubscriptionOption, Boolean, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |
| `purchasePackageWith(Activity, Package, UpgradeInfo, Boolean, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`                                     |
| `purchasePackageWith(Activity, Package, Boolean, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`                                                  |
| `purchaseProductWith(Activity, StoreProduct, Boolean, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`                                             |
| `purchaseProductWith(Activity, StoreProduct, UpgradeInfo, Boolean, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`                                |

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
