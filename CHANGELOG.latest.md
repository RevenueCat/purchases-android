> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### 🐞 Bugfixes
* Remove GOOGLE_PRODUCT_MISSING_INFINITELY_RECURRING_BILLING_PHASE log message (#2767) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Invalidate CustomerInfo cache when opening a WPB in paywalls (#2785) via Toni Rico (@tonidero)
* FIX: Video component - felt slow on first appearance (#2784) via Jacob Rakidzich (@JZDesign)
* Add package fallback to tab selection when there's no default selected (#2782) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Add golden tests for requests and responses in backend integration tests (#2781) via Toni Rico (@tonidero)
* Unlock entitlements from add-ons in offline entitlements mode (#2776) via Will Taylor (@fire-at-will)
* Add fallback URL integration tests (#2769) via Toni Rico (@tonidero)
* [SDK-4152] Use static fallback urls (#2780) via Antonio Pallares (@ajpallares)
* Skip tests not supported in fortress (#2783) via Toni Rico (@tonidero)
* Keep cache logic similar to iOS + ensure path extension is on file (#2750) via Jacob Rakidzich (@JZDesign)
* Use cached offerings on network errors (#2772) via Toni Rico (@tonidero)
* [EXTERNAL] Update Emerge snapshots to best/latest practices (#2777) by @rbro112 (#2779) via Toni Rico (@tonidero)
* Change ForceServerErrorStrategy to be a `fun interface` to simplify instantiation (#2773) via Toni Rico (@tonidero)
* Stores backend integration test result on CircleCI (#2771) via JayShortway (@JayShortway)
* Change test server down mechanism to use new server down endpoint (#2765) via Toni Rico (@tonidero)
