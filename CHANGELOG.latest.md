- Fix product price and currency not being reported in Amazon Appstore observer mode. `syncObserverModeAmazonPurchase` 
  now accepts an `isoCurrencyCode` and a `price` to make sure we correctly track prices. 
  https://github.com/RevenueCat/purchases-android/pull/519
