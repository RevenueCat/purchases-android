> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 📦 Dependency Updates
* [RENOVATE] Update build-dependencies to v8.13.2 (#2929) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix `IllegalStateException`: No `ViewModelStoreOwner` was provided via `LocalViewModelStoreOwner` in `CompatComposeView` (#2912) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Change AdFailedToLoad mediatorErrorCode type from Long to Int (#2924) via Pol Miro (@polmiro)
* Change paywall preview recorder name template (#2923) via Toni Rico (@tonidero)
* Don't use reflection to instantiate AmazonDeviceIdentifiersFetcher (#2919) via Will Taylor (@fire-at-will)
