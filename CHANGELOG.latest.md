> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Hotfix video component crash onPause (#2804) via Jacob Rakidzich (@JZDesign)
* Normalize product ids (#2797) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Adds `TestStoreErrorDialogActivity` (#2809) via JayShortway (@JayShortway)
* Update golden test files to have tests pass again (#2813) via Toni Rico (@tonidero)
* Wrap error when connecting to BillingClient (#2801) via Toni Rico (@tonidero)
* Use new fortress us-east-1 base url (#2805) via Toni Rico (@tonidero)
* Support fortress us-east-2 integration tests (#2793) via Toni Rico (@tonidero)
* Add test backend environment parameter to integration tests (#2792) via Toni Rico (@tonidero)
* Unify integration tests jobs (#2791) via Toni Rico (@tonidero)
* Update CHANGELOG to reflect where changes actually happened in 9.12.0 and 9.12.1 (#2798) via Toni Rico (@tonidero)
* Add extra request logging (#2794) via Toni Rico (@tonidero)
* Fix golden backend integration tests (#2795) via Toni Rico (@tonidero)
