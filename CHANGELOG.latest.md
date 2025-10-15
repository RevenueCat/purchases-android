> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### ✨ New Features
* Turn on Video Component (#2725) via Jacob Rakidzich (@JZDesign)

## RevenueCatUI SDK
### Customer Center
#### 🐞 Bugfixes
* Fix potential `NoClassDefFoundError` crash finding `Icons` caused by material3 1.4.0  (#2727) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Add test store maestro test (#2711) via Toni Rico (@tonidero)
* Finalize video component to include a checksum, and optimize memory usage for large file downloads (#2717) via Jacob Rakidzich (@JZDesign)
