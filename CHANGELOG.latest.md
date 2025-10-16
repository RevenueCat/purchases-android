> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### ✨ New Features
* Adds new experimental `storefrontLocale` APIs. (#2726) via JayShortway (@JayShortway)

## RevenueCatUI SDK
### Customer Center
#### 🐞 Bugfixes
* Fixes more Icons crashing with `NoClassDefFoundError` when using Material 1.4.0 (#2732) via Cesar de la Vega (@vegaro)
