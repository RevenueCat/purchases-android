## v6 API Changes

### Classes

| New                        |
|----------------------------|
| `SubscriptionOption`       |
| `GoogleSubscriptionOption` |
| `GoogleStoreProduct`       |
| `AmazonStoreProduct`       |
| `Price`                    |
| `PricingPhase`             |
| `RecurrenceMode`           |
| `GoogleProrationMode`      |

| Old Location                            | New Location                                   |
|-----------------------------------------|------------------------------------------------|
| com.revenuecat.purchases.BillingFeature | com.revenuecat.purchases.models.BillingFeature |

### StoreProduct

StoreProduct has been made an interface, which `GoogleStoreProduct` and `AmazonStoreProduct` implement.

| Previous                      | New                                                                           |
|-------------------------------|-------------------------------------------------------------------------------|
| price                         | Moved to `price`                                                              |
| priceAmountMicros             | Moved to `price`                                                              |
| priceCurrencyCode             | Moved to `price`                                                              |
| freeTrialPeriod               | Moved to PricingPhase                                                         |
| introductoryPrice             | Moved to PricingPhase                                                         |
| introductoryPriceAmountMicros | Moved to PricingPhase                                                         |
| introductoryPricePeriod       | Moved to PricingPhase                                                         |
| introductoryPriceCycles       | Moved to PricingPhase                                                         |
| originalJson                  | `productDetails` in GoogleStoreProduct; `amazonProduct` in AmazonStoreProduct |

| New                 |
|---------------------|
| price               |
| subscriptionOptions |
| defaultOption       |

| Removed                           |                                                                                                                                                     | 
|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| product.originalPrice             | Accessible in basePlan's unique pricing phase `subscriptionOptions.firstOrNull{ it.isBasePlan }?.pricingPhases?.firstOrNull()?.price?.formatted`    |
| product.originalPriceAmountMicros | Accessible in basePlan's unique pricing phase `subscriptionOptions.firstOrNull{ it.isBasePlan }?.pricingPhases?.firstOrNull()?.price?.amountMicros` |
| product.iconUrl                   |                                                                                                                                                     | 
| product.originalJson              | (product as GoogleStoreProduct).productDetails                                                                                                      |

#### Free Trial and Introductory Offers

`StoreProduct` can now have multiple free trials and introductory offers. There is a `defaultOption` property on
`StoreProduct` that will select the offer with the longest free trial period or the cheapest introductory offer.

If more than that is needed, the free trial and intro offer options can be found like:

```kotlin
val freeOption = storeProduct.subscriptionOptions?.firstOrNull { it.freePhase != null }
val trialOption = storeProduct.subscriptionOptions?.firstOrNull { it.introPhase != null }
```

### StoreTransaction

| New                  |
|----------------------|
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

The `purchasePackage()` and `purchaseProduct()` APIs have a new behavior for selecting which offer is used when
purchasing a `Package` or `StoreProduct`. These functions use the following logic to choose
a [SubscriptionOption] to purchase:
*   - Filters out offers with "rc-ignore-default-offer" tag
*   - Uses [SubscriptionOption] WITH longest free trial or cheapest first phase
*   - Falls back to use base plan

For more control, the `purchaseSubscriptionOption()` API can be used to manually choose which offer to purchase.

| New                                                                                                      |
|----------------------------------------------------------------------------------------------------------|
| `purchaseSubscriptionOption(Activity, StoreProduct, SubscriptionOption, PurchaseCallback)`                   |
| `purchaseSubscriptionOption(Activity, StoreProduct, SubscriptionOption, UpgradeInfo, ProductChangeCallback)` |

| Deprecated                                                       | New                                                   |
|------------------------------------------------------------------|-------------------------------------------------------|
| `getSubscriptionSkus(List<String>, GetStoreProductsCallback)`    | `getProducts(List<String>, GetStoreProductsCallback)` |
| `getNonSubscriptionSkus(List<String>, GetStoreProductsCallback)` | `getProducts(List<String>, GetStoreProductsCallback)` |

### Kotlin Helpers

| New                                                                                                                                                                |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `purchaseSubscriptionOptionWith(Activity, StoreProduct, SubscriptionOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`              |
| `purchaseSubscriptionOptionWith(Activity, StoreProduct, UpgradeInfo, SubscriptionOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |

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
