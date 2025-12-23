> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### 🐞 Bugfixes
* Retry creation of font folder (#2965) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Improve event tracking reliability (#2968) via Toni Rico (@tonidero)
* Use @ExperimentalPreviewRevenueCatPurchasesAPI for non subscription tracking API (#2962) via Pol Miro (@polmiro)
