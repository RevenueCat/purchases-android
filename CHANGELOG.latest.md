> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Make close() method also clear the shared instance of the SDK (#2940) via Toni Rico (@tonidero)
* Fix purchase callback not firing for DEFERRED product changes with baePlanId in oldProductId (#2937) via Facundo Menzella (@facumenzella)

### 🔄 Other Changes
* [AUTOMATIC] Update golden test files for backend integration tests (#2944) via RevenueCat Git Bot (@RCGitBot)
* Adds `.kotlin` to `.gitignore` (#2941) via JayShortway (@JayShortway)
* Dont use reflection to instantiate AmazonOfferingParser (#2934) via Will Taylor (@fire-at-will)
