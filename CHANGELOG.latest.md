> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Fix missing events tracking by removing delay to flush when backgrounding app (#2907) via Toni Rico (@tonidero)

### 🔄 Other Changes
* Update Golden File for Galaxy Store (#2902) via Will Taylor (@fire-at-will)
* Improve log message for no products of specific store in any offerings (#2897) via Antonio Pallares (@ajpallares)
