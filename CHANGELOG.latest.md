### Bugfixes
* Remove flavors temporarily to fix deployment (#953) via Toni Rico (@tonidero)
### Other Changes
* Add diagnostics tracking for BC5 google methods (#876) via Toni Rico (@tonidero)
### Changes from 6.1.0
#### New Features
* Add `OfferPaymentMode` on to `PricingPhase` to describe behavior for offers (#943) via Josh Holtz (@joshdholtz)
* Add `Purchases.sharedInstance.store` to get currently configured `Store` (#937) via Josh Holtz (@joshdholtz)
#### Bugfixes
* Add missing presentedOfferingId to `StoreProduct` and `SubscriptionOption` (#930) via beylmk (@beylmk)
* Moved `toPurchasesError` back to internal (#944) via Cesar de la Vega (@vegaro)
#### Other Changes
* Compute CustomerInfo from a list of purchases (#885) via Cesar de la Vega (@vegaro)
* Extract all customer info json strings (#945) via Cesar de la Vega (@vegaro)
* Add verification for entitlement granting to integration tests (#946) via Toni Rico (@tonidero)
* Add note about observer mode to v6-migration (#929) via beylmk (@beylmk)
* Report load shedder integration tests results in slack for V6 (#936) via Toni Rico (@tonidero)
* Increase heap size (#935) via Cesar de la Vega (@vegaro)
* Re-add new integration tests after V6 changes (#928) via Toni Rico (@tonidero)
