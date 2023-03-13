## v6 API Changes

### Classes

| New                        |
|----------------------------|
| `AmazonStoreProduct`       |
| `GoogleProrationMode`      |
| `GoogleStoreProduct`       |
| `GoogleSubscriptionOption` |
| `Period`                   |
| `Period.Unit`              |
| `Price`                    |
| `PricingPhase`             |
| `RecurrenceMode`           |
| `SubscriptionOption`       |
| `SubscriptionOptions`      |

| Old Location                            | New Location                                   |
|-----------------------------------------|------------------------------------------------|
| com.revenuecat.purchases.BillingFeature | com.revenuecat.purchases.models.BillingFeature |


| Deprecated              | Replace with                                                                           |
|-------------------------|----------------------------------------------------------------------------------------|
| `UpgradeInfo`           | `PurchaseParams.Builder.oldProductId` and `PurchaseParams.Builder.googleProrationMode` |
| `ProductChangeCallback` | `PurchaseCallback`                                                                     |


### StoreProduct changes

`StoreProduct` has been made an interface, which `GoogleStoreProduct` and `AmazonStoreProduct` implement. 
`StoreProduct` is no longer `Parcelable`.

| Previous                      | New                                                                           |
|-------------------------------|-------------------------------------------------------------------------------|
| sku                           | id                                                                            |
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

If more control is needed, the free trial, intro offer, and other [SubscriptionOption]s can
be found through `subscriptionOptions`:

```kotlin
val basePlan = storeProduct.subscriptionOptions?.basePlan
val defaultOffer = storeProduct.subscriptionOptions?.defaultOffer
val freeOffer = storeProduct.subscriptionOptions?.freeTrial
val trialOffer = storeProduct.subscriptionOptions?.introTrial
val offersForLapsedCustomers = storeProduct.subscriptionOptions?.withTag("lapsed-customers")
```

### Period and Period.Unit changes

Durations were previously string properties of ISO 8601 durations (ex: "P4D", "P1M", "P1Y").
All durations are now stored in a `Period` object with the following properties:

| New     |
|---------|
| value   |
| unit    |
| iso8601 | 

The `Period.Unit` enum can be one of the following:
- DAY
- WEEK
- MONTH
- YEAR
- UNKNOWN

### StoreTransaction changes

| New                  |
|----------------------|
| subscriptionOptionId |

| Deprecated | New        |
|------------|------------|
| skus       | productIds |


### CustomerInfo changes

| Deprecated              | New                           |
|-------------------------|-------------------------------|
| allPurchasedSkus        | allPurchasedProductIds        |
| getExpirationDateForSku | getExpirationDateForProductId |
| getPurchaseDateForSku   | getPurchaseDateForProductId   |

### Purchasing API changes

Purchases are now configured using `PurchaseParams.Builder()`. The builder is constructed with either a `Package`,
`Product`, or `SubscriptionOption` and an `Activity`. The builder then has methods for setting the product change
parameters (`oldProductId` and `googleProrationMode`).

To initiate a purchase, simply pass the built `PurchaseParams` and your `PurchaseCallback` to the `purchase()` method.

#### Applying offers on a purchase
In v5, a purchase of a `Package` or `StoreProduct` represented a single purchaseable entity, and free trials or intro
prices would automatically be applied if the user was eligible.

Now, in v6, a `Package` or `StoreProduct` could contain multiple offers along with a base plan. 
When passing a `Package` or `StoreProduct` to `purchase()`, the SDK will use the following logic to choose which 
[SubscriptionOption] to purchase:
*   - Filters out offers with "rc-ignore-default-offer" tag
*   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
*   - Falls back to use base plan

For more control, create your `PurchaseParams.Builder` with the desired `SubscriptionOption`.

| New                                          |
|----------------------------------------------|
| `purchase(PurchaseParams, PurchaseCallback)` |

Replaces all of the following: 

| Deprecated                                                                    |
|-------------------------------------------------------------------------------|
| `purchaseProduct(Activity, StoreProduct, PurchaseCallback)`                   |
| `purchaseProduct(Activity, StoreProduct, UpgradeInfo, ProductChangeCallback)` |
| `purchasePackage(Activity, Package, PurchaseCallback)`                        |
| `purchasePackage(Activity, Package, UpgradeInfo, ProductChangeCallback)`      |

#### Kotlin Helpers

| New                                                                                                         |
|-------------------------------------------------------------------------------------------------------------|
| `purchaseWith(PurchaseParams, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |

Replaces all of the following:

| Deprecated                                                                                                                               |
|------------------------------------------------------------------------------------------------------------------------------------------|
| `purchaseProductWith(Activity, StoreProduct, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`               |
| `purchaseProductWith(Activity, StoreProduct, UpgradeInfo, (PurchasesError, Boolean) -> Unit, (StoreTransaction?, CustomerInfo) -> Unit)` |
| `purchasePackageWith(Activity, Package, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`                    |
| `purchasePackageWith(Activity, Package, UpgradeInfo, (PurchasesError, Boolean) -> Unit, (StoreTransaction?, CustomerInfo) -> Unit)`      |

### Purchases API deprecations

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
