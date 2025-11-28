> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix ANR when initializing FontLoader (#2819) via Cesar de la Vega (@vegaro)
### Paywallv2
#### 🐞 Bugfixes
* Fix `Template7CustomPackagesTestData` (#2875) via Cesar de la Vega (@vegaro)
* Fix predownloading of fonts if first offering doesn't have paywall components (#2873) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Extract parameters for non paid revenue tracking API to use objects (#2871) via Toni Rico (@tonidero)
* Bump fastlane from 2.229.0 to 2.229.1 (#2869) via dependabot[bot] (@dependabot[bot])
