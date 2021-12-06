- Removed deprecated `purchaseProduct` and `purchasePackage` accepting `UpgradeInfo` and `MakePurchaseListener`
- `identify` has been removed
- `reset` has been removed
- `createAlias` has been removed
- `Package.product` has been changed from `SkuDetails` to `StoreProduct`. 
- `configure` accepts a Builder now. Previous function deprecated. 
- `purchasePackage` and `purchaseProduct` callbacks has been changed to return a `PaymentTransaction` instead of a `Purchase`. 
  This means that`MakePurchaseListener` deprecated in favor of `PurchaseCallback`. 
  You can use `MakePurchaseListener.toPurchaseCallback()` in Kotlin for an easy migration.
  For purchasing functions that accept an `UpgradeInfo`, `ProductChangeListener` has been deprecated in favor of `ProductChangeCallback`. 
  You can use `ProductChangeListener.toProductChangeCallback()` and `MakePurchaseListener.toProductChangeCallback()` in Kotlin for an easy migration.
- `getSubscriptionSkus` and `getNonSubscriptionSkus` callbacks has been changed to return `StoreProduct` objects instead 
  of `SkuDetails`. This means that `GetSkusResponseListener` has been deprecated in favor of `GetStoreProductCallback`. 
  You can use `GetSkusResponseListener.toGetStoreProductCallback()` in Kotlin for an easy migration.  
- `getSubscriptionSkusWith` and `getNonSubscriptionSkusWith` now receive `storeProducts` instead of `skus`.
- `purchaseProductWith` now accepts a `StoreProduct` instead of a`SkuDetails`
