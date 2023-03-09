## 5.8.2

⚠️ ⚠️ ⚠️ ⚠️ 

Our Android SDK versions 5.8.0 and 5.8.1 introduced a bug that prevented new purchase tokens from being sent to RevenueCat for validation between the above dates.

Users who made new purchases were charged by Google but did not receive entitlements during that time. We've already shipped a backend fix, so affected users can recover their purchases simply by opening the app again. If the purchases aren’t recovered by the users within 72 hours from their purchase date, Google will automatically refund their purchase. No further action is required from you at this time.

Users with pre-existing purchases are not affected.

⚠️ ⚠️ ⚠️ ⚠️

### Other changes in 5.8.0 and 5.8.1
### New Features
* Diagnostics (#811) via Toni Rico (@tonidero)
### Bugfixes
* Fix issue with missing subscriber attributes if set after login but before login callback (#809) via Toni Rico (@tonidero)
### Dependency Updates
* Bump fastlane from 2.212.0 to 2.212.1 (#821) via dependabot[bot] (@dependabot[bot])
* Bump fastlane from 2.211.0 to 2.212.0 (#808) via dependabot[bot] (@dependabot[bot])
* Bump fastlane-plugin-versioning_android from 0.1.0 to 0.1.1 (#798) via dependabot[bot] (@dependabot[bot])
* Bump danger from 8.6.1 to 9.2.0 (#778) via dependabot[bot] (@dependabot[bot])
### Other Changes
* Fix docs deployment (#836) via Toni Rico (@tonidero)
* Fix SDKMAN issues (#822) via Toni Rico (@tonidero)
* Fix bundle install on CircleCI (#827) via Cesar de la Vega (@vegaro)
* Update README.md to include minimum Kotlin version (#786) via Cesar de la Vega (@vegaro)
* Remove `tag_release_with_latest_if_needed` fastlane lane (#781) via Cesar de la Vega (@vegaro)
* Adds docs for timeouts when closing and releasing (#759) via Cesar de la Vega (@vegaro)
* Add Amazon App tester package to purchase tester queries (#789) via Stefan Wehner (@tonidero)
