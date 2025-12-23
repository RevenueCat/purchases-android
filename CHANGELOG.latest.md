> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Remove deprecated API usage (#2957) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Fix crash when context.cacheDir is null in FontLoader (#2961) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Bump fastlane from 2.229.1 to 2.230.0 (#2951) via dependabot[bot] (@dependabot[bot])
