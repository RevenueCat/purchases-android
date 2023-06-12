# V5 API Migration Guide

There were various updates to our API in this release, in order to best support the Amazon Appstore and to maintain parity 
with our iOS SDK, which recently [migrated from ObjC to Swift](https://revenuecat.github.io/purchases-ios-docs/v4_api_migration_guide.html).

## Type Changes
- `PurchaserInfo` has been deprecated and renamed to `CustomerInfo`. This rename also affects to all functions that had
  `PurchaserInfo` in their name, like `getPurchaserInfo` which has been renamed to `getCustomerInfo`.
- `ReceiveOfferingsListener` and `PurchasesErrorListener` have been renamed to `ReceiveOfferingsCallback` and `PurchasesErrorCallback`
- `Package.product` has been changed from being a `SkuDetails` to `StoreProduct`.

| Old type name | New type name |
|------------|------|
| `PurchaserInfo` | `CustomerInfo` |
| `ReceivePurchaserInfoListener` | `ReceiveCustomerInfoCallback` |
| `UpdatedPurchaserInfoListener` | `UpdatedCustomerInfoListener` |
| `ReceiveOfferingsListener` | `ReceiveOfferingsCallback` |
| `PurchasesErrorListener` | `PurchasesErrorCallback` |

## Deprecated + New APIs

- The `configure` function has been changed to accept a `PurchasesConfiguration.Builder`. The previous function is deprecated. The new function can be used like this:

```
Purchases.configure(PurchasesConfiguration.Builder(this, "public_google_sdk_key").build())
```

or for Amazon:

```
Purchases.configure(AmazonConfiguration.Builder(this, "public_amazon_sdk_key").build())
```
- `purchasePackage` and `purchaseProduct` callbacks have been changed to return a `StoreTransaction` instead of a `Purchase` in the `onCompleted`.
  This means that `MakePurchaseListener` deprecated in favor of `PurchaseCallback`.
  There is a helper extension function `MakePurchaseListener.toPurchaseCallback()` that can help migrate.
  For purchasing functions that accept an `UpgradeInfo`, `ProductChangeListener` has been deprecated in favor of `ProductChangeCallback`.
  Similarly, you can use `ProductChangeListener.toProductChangeCallback()` and `MakePurchaseListener.toProductChangeCallback()` in Kotlin for an easy migration.
  Due to the same change, Kotlin helper `purchaseProductWith` now accepts a `StoreProduct` instead of a`SkuDetails`
- `getSubscriptionSkus` and `getNonSubscriptionSkus` callbacks has been changed to return `StoreProduct` objects instead
  of `SkuDetails`. This means that `GetSkusResponseListener` has been deprecated in favor of `GetStoreProductsCallback`.
  You can use `GetSkusResponseListener.toGetStoreProductsCallback()` in Kotlin for an easy migration.
  For the same reasons, `getSubscriptionSkusWith` and `getNonSubscriptionSkusWith` now receive `storeProducts` instead of `skus`.


| Deprecated | New  |
|------------|------|
| `configure(Context, String, String?, Boolean, ExecutorService)` | `configure(PurchasesConfiguration)` |
| `invalidatePurchaserInfoCache()` | `invalidateCustomerInfoCache()` |
| `removeUpdatedPurchaserInfoListener()` | `removeUpdatedCustomerInfoListener()` |
| `getPurchaserInfo(ReceivePurchaserInfoListener)` | `getCustomerInfo(ReceiveCustomerInfoCallback)` |
| `restorePurchases(ReceivePurchaserInfoListener)` | `restorePurchases(ReceiveCustomerInfoCallback)` |
| `logOut(ReceivePurchaserInfoListener)` | `logOut(ReceiveCustomerInfoCallback)` |
| `purchaseProduct(Activity, SkuDetails, MakePurchaseListener)` | `purchaseProduct(Activity, StoreProduct, PurchaseCallback)` |
| `purchaseProduct(Activity, SkuDetails, UpgradeInfo, ProductChangeListener)` | `purchaseProduct(Activity, StoreProduct, UpgradeInfo, ProductChangeCallback)` |
| `purchasePackage(Activity, Package, MakePurchaseListener)` | `purchasePackage(Activity, Package, PurchaseCallback)` |
| `purchasePackage(Activity, Package, UpgradeInfo, ProductChangeListener)` | `purchasePackage(Activity, Package, UpgradeInfo, ProductChangeCallback)` |
| `getSubscriptionSkus(List<String>, GetSkusResponseListener)` | `getSubscriptionSkus(List<String>, GetStoreProductsCallback)` |
| `getNonSubscriptionSkus(List<String>, GetSkusResponseListener)` | `getNonSubscriptionSkus(List<String>, GetStoreProductsCallback)` |

## Kotlin Helpers Changes

| Old signature | New signature |
|---------------|---------------|
| `getPurchaserInfoWith((PurchasesError) -> Unit, (PurchaserInfo) -> Unit)` | `getCustomerInfoWith((PurchasesError) -> Unit, (CustomerInfo) -> Unit)` |
| `purchasePackageWith(Activity, Package, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)` | `purchasePackageWith(Activity, Package, (PurchasesError) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |
| `purchasePackageWith(Activity, Package, UpgradeInfo, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)` | `purchasePackageWith(Activity, Package, UpgradeInfo, (PurchasesError) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |
| `purchaseProductWith(Activity, SkuDetails, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)` | `purchaseProductWith(Activity, StoreProduct, (PurchasesError) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |
| `purchaseProductWith(Activity, SkuDetails, UpgradeInfo, (PurchasesError) -> Unit, (Purchase, PurchaserInfo) -> Unit)` | `purchaseProductWith(Activity, StoreProduct, UpgradeInfo, (PurchasesError) -> Unit, (StoreTransaction, CustomerInfo) -> Unit)` |

## Removed APIs
- Some deprecated functions have been removed: `identify`, `reset`, `createAlias`, `isBillingSupported`, `isFeatureSupported`, `addAttributionData` and the versions of `purchaseProduct`/`purchasePackage` accepting `UpgradeInfo` and `MakePurchaseListener`

| Removed APIs |  
|---------------------------------------------------------------------------|
| `purchaseProduct(Activity, SkuDetails, UpgradeInfo, MakePurchaseListener)` |
| `purchasePackage(Activity, Package, UpgradeInfo, MakePurchaseListener)` |
| `createAlias(String, ReceivePurchaserInfoListener?)` |
| `identify(String, ReceivePurchaserInfoListener?)` |
| `reset(ReceivePurchaserInfoListener?)` |
| `createAliasWith(String, (PurchasesError) -> Unit, (PurchaserInfo) -> Unit)` |
| `identifyWith(String, (PurchasesError) -> Unit, (PurchaserInfo) -> Unit)` |
| `resetWith((PurchasesError) -> Unit, (PurchaserInfo) -> Unit)` |
| `isBillingSupported(Context, Callback<Boolean>)` |
| `isFeatureSupported(BillingClient.FeatureType, Context, Callback<Boolean>)` |
| `addAttributionData(JSONObject, AttributionNetwork, String?)` |
| `addAttributionData(Map<String, Any?>, AttributionNetwork, String?)` |
| `AttributionNetwork` |

## Other changes:

- Our library now requires Java 8
- Amazon support (see [the section in our docs for more information](https://docs.revenuecat.com/docs/amazon-platform-resources))

## Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
