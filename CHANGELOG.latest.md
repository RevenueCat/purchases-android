## RevenueCat SDK
### 🐞 Bugfixes
* Fixes incorrect lifecycle transitions in `CompatComposeView` (#3006) via JayShortway (@JayShortway)
* Fixes `showInAppMessages` NPE when the Activity has no content View (#3004) via JayShortway (@JayShortway)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Synchronize `cacheDirectory` lazy property to fix `NullPointerException` on `UnsafeLazyImpl`  (#3019) via Cesar de la Vega (@vegaro)
### Customer Center
#### 🐞 Bugfixes
* Simplify determining paid price in Customer Center (#2600) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Improve accuracy of transactions origin Part 6: Cleanup, Do not cache user id (#3012) via Toni Rico (@tonidero)
* Properly cleanup new shared preferences files between tests (#3016) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#3015) via RevenueCat Git Bot (@RCGitBot)
* Improve accuracy of transactions origin Part 5: Cleanup (#3002) via Toni Rico (@tonidero)
* Improve accuracy of transactions origin Part 4: Post remaining transaction metadata when syncing purchases (#2993) via Toni Rico (@tonidero)
* Improve accuracy of transactions origin Part 3: Merge cached data when posting receipts + Cache amazon data (#2989) via Toni Rico (@tonidero)
* Improve accuracy of transactions origin Part 2: Store transaction metadata (#2988) via Toni Rico (@tonidero)
* Improve accuracy of transactions origin Part 1: Refactor to allow caching transaction metadata (#2987) via Toni Rico (@tonidero)
* [AUTOMATIC] Update golden test files for backend integration tests (#3010) via RevenueCat Git Bot (@RCGitBot)
* [AUTOMATIC] Update golden test files for backend integration tests (#3008) via RevenueCat Git Bot (@RCGitBot)
* Track exit offers (#2975) via Cesar de la Vega (@vegaro)
