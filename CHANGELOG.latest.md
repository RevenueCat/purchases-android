> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCat SDK
### ✨ New Features
* Add webCheckoutURL property to Offering and Package (#2747) via Toni Rico (@tonidero)
### 🐞 Bugfixes
* Use the fallback endpoint in more failure scenarios (#2754) via JayShortway (@JayShortway)
* Protect against developers excluding blockstore dependency (#2745) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* Add support for Web Purchase buttons in paywalls (#2751) via Toni Rico (@tonidero)
### Customer Center
#### 🐞 Bugfixes
* Add elevation to surfaces in Customer Center to fix default appearance (#2741) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* [Experimental]: Support Multi-Line Subscriptions (#2668) via Will Taylor (@fire-at-will)
* Add PurchaseButtonComponent WPL field deserialization (#2749) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `25c7fb8` to `525d48c` (#2748) via dependabot[bot] (@dependabot[bot])
* Add extra message test store dialog (#2746) via Toni Rico (@tonidero)
