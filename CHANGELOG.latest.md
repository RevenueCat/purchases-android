### New Features
* New SubscriptionOptions model  (#816) via Josh Holtz (@joshdholtz)
* StoreProduct id improvements (#810) via Josh Holtz (@joshdholtz)
* New Period and Unit classes to represent ISO8601 period strings (#806) via Josh Holtz (@joshdholtz)
* Price API improvements (#794) via Josh Holtz (@joshdholtz)
### Bugfixes
* Found and fixed logMissingProducts bug where never any missing products (#813) via Josh Holtz (@joshdholtz)
* Fix where recurring and prepaid subs of same duration where considered same StoreProduct (#814) via Josh Holtz (@joshdholtz)
* Fix period serialization (#818) via swehner (@swehner)
* Fix value of NON_RECURRING from 0 to 3 (#797) via Josh Holtz (@joshdholtz)
* Fix callback error issue (#732) via beylmk (@beylmk)
### Other Changes
* Small doc change for StoreProduct.title explaining why duplicate titles when using base plans on same subscription (#815) via Josh Holtz (@joshdholtz)
* Migration guide updates for API improvements (#829) via Josh Holtz (@joshdholtz)
* Fix serialization of period (#818) via Stefan Wehner (@swehner)
