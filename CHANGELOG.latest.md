> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* [Purchase Tester]: Fix toggling "Base Item" box sometimes programmatically enabling other "Buy Option" boxes (#2812) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* Add hook to delay payment screens (#2753) via Jacob Rakidzich (@JZDesign)
* Video Background (#2786) via Jacob Rakidzich (@JZDesign)
### Customer Center
#### 🐞 Bugfixes
* Fix dark mode in CustomerCenterView (#2817) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Fix integration tests (#2821) via Toni Rico (@tonidero)
* Add internal CustomerInfo source properties (#2796) via Toni Rico (@tonidero)
