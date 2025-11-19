> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* CC-581 | Allow for support ticket creation (#2810) via Rosie Watson (@RosieWatson)

### 🔄 Other Changes
* Uses some git+GitHub lanes from Fastlane plugin (#2856) via JayShortway (@JayShortway)
* Add client side timeout logic for endpoints that support fallback URLs (#2807) via Toni Rico (@tonidero)
* [EXTERNAL] Fix deprecation warnings in examples module (#2852) contributed by @gojoel (#2853) via Toni Rico (@tonidero)
* Bump fastlane-plugin-revenuecat_internal from `9f78bb9` to `083ced9` (#2848) via dependabot[bot] (@dependabot[bot])
