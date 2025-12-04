> [!WARNING]  
> If you don't have any login system in your app, please make sure your one-time purchase products have been correctly configured in the RevenueCat dashboard as either consumable or non-consumable. If they're incorrectly configured as consumables, RevenueCat will consume these purchases. This means that users won't be able to restore them from version 9.0.0 onward.
> Non-consumables are products that are meant to be bought only once, for example, lifetime subscriptions.


## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Fix exception when buttons are hidden (#2889) via Cesar de la Vega (@vegaro)
* Fixes `ConcurrentModificationException` in `FontLoader` (#2885) via JayShortway (@JayShortway)

### 🔄 Other Changes
* Add extra non subscription events (#2892) via Toni Rico (@tonidero)
* Show redacted Test Api key in alert when detected in Release build (#2891) via Antonio Pallares (@ajpallares)
* Remove data binding purchase tester + Update AGP (#2887) via Toni Rico (@tonidero)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2828) via RevenueCat Git Bot (@RCGitBot)
* Remove unused `BuildConfig. ENABLE_VIDEO_COMPONENT ` (#2884) via Toni Rico (@tonidero)
* Configure build logic and restructure gradle files (#2827) via Jaewoong Eum (@skydoves)
