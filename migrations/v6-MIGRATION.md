## v6 API changes

### New

| Name                   |
|------------------------|
| `PurchaseOption`       |
| `GooglePurchaseOption` |
| `GoogleStoreProduct`   |
| `AmazonStoreProduct`   |
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

### Deprecated + New APIs

| Deprecated                                                                    | New                                                                                                 |
|-------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `purchaseProduct(Activity, StoreProduct, PurchaseCallback)`                   | `purchaseProductOption(Activity, StoreProduct, PurchaseOption, PurchaseCallback)`                   |
| `purchaseProduct(Activity, StoreProduct, UpgradeInfo, ProductChangeCallback)` | `purchaseProductOption(Activity, StoreProduct, PurchaseOption, UpgradeInfo, ProductChangeCallback)` |
| `purchasePackage(Activity, Package, PurchaseCallback)`                        | `purchasePackageOption(Activity, Package, PurchaseOption, PurchaseCallback)`                        |
| `purchasePackage(Activity, Package, UpgradeInfo, ProductChangeCallback)`      | `purchasePackage(Activity, Package, PurchaseOption, UpgradeInfo, ProductChangeCallback)`            |


### Kotlin Helpers Changes

| Old signature                                                                                                                           | New signature                                                                                                                                                 |
|-----------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `purchaseProductWith(Activity, StoreProduct, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`              | `purchaseProductOptionWith(Activity, StoreProduct, PurchaseOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`              |
| `purchaseProductWith(Activity, StoreProduct, UpgradeInfo, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` | `purchaseProductOptionWith(Activity, StoreProduct, UpgradeInfo, PurchaseOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |
| `purchasePackageWith(Activity, Package, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`                   | `purchasePackageOptionWith(Activity, Package, PurchaseOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`                   |
| `purchasePackageWith(Activity, Package, UpgradeInfo, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`      | `purchasePackageOptionWith(Activity, Package, UpgradeInfo, PurchaseOption, (PurchasesError, Boolean) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)`      |

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
