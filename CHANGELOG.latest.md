## RevenueCat SDK
### 🐞 Bugfixes
* Fix & Standardize Galaxy Date Parsing Edge Cases (#3216) via Will Taylor (@fire-at-will)
* Fix addSuccessfullyPostedToken for new purchases in PostPendingTransactionsHelper (#3239) via Facundo Menzella (@facumenzella)
* [Galaxy]: Fix race condition when fetching Galaxy products (#3213) via Will Taylor (@fire-at-will)
* Fixes double padding in PaywallActivity on Android 15+ when `edgeToEdge` parameter is false (#3227) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### 🐞 Bugfixes
* Fix bold text not rendering in Markdown lists (#3228) via Cesar de la Vega (@vegaro)
* Fix: Clear in-memory offerings cache on locale override to prevent stale paywall data (#3225) via Antonio Pallares (@ajpallares)
### Paywallv2
#### ✨ New Features
* Feature: Update default paywall (#3133) via Jacob Rakidzich (@JZDesign)
#### 🐞 Bugfixes
* Fix V2 paywall safe area in landscape mode (#3221) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Run integration tests on all branches (#3242) via Toni Rico (@tonidero)
* Migrate Firebase Test Lab jobs to CircleCI emulators (#3238) via Toni Rico (@tonidero)
* Run metalava on galaxy module in test-galaxy job (#3235) via Will Taylor (@fire-at-will)
* Add offering_id to custom paywall impression event (#3230) via Rick (@rickvdl)
* Cache isAutoRenewing to detect subscription changes without syncPurchases (#3198) via Facundo Menzella (@facumenzella)
* Bump fastlane-plugin-revenuecat_internal from `e146447` to `3e8c384` (#3233) via dependabot[bot] (@dependabot[bot])
