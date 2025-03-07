## RevenueCat SDK
### ✨ New Features
* Add `hasPaywall` property to `Offering` (#2212) via Antonio Pallares (@ajpallares)
### 🐞 Bugfixes
* Fix empty options in NoActive subscriptions screen (#2168) via Cesar de la Vega (@vegaro)

## RevenueCatUI SDK
### Customer Center
#### ✨ New Features
* Create `CustomerCenterListener` (#2199) via Cesar de la Vega (@vegaro)
#### 🐞 Bugfixes
* Reload Customer Center after a successful restore (#2203) via Cesar de la Vega (@vegaro)
* Fixes CustomerCenter state not refreshing when reopening (#2202) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Improves PaywallsTester multi-API-key support (#2218) via JayShortway (@JayShortway)
* [EXTERNAL] Bump Emerge Gradle Plugin and Snaphsots version (#2211) via @runningcode (#2217) via JayShortway (@JayShortway)
* [AUTOMATIC][Paywalls V2] Updates Compose previews of all templates (#2207) via RevenueCat Git Bot (@RCGitBot)
* [Paywalls V2] Enables template previews again (#2215) via JayShortway (@JayShortway)
* Adds support for switching between 2 API keys to PaywallsTester (#2213) via JayShortway (@JayShortway)
* Adds a `LocalPreviewImageLoader` `CompositionLocal`. (#2201) via JayShortway (@JayShortway)
* Logs from RevenueCatUI are now tagged with `[Purchases]` too. (#2206) via JayShortway (@JayShortway)
* [Paywalls V2] Ignores template previews for now. (#2209) via JayShortway (@JayShortway)
* [Paywalls V2] Some more template previews optimizations (#2208) via JayShortway (@JayShortway)
* chore: Delete key from customer center survey event (#2204) via Facundo Menzella (@facumenzella)
* [Paywalls V2] Extract ImageLoader to use a single one (#2146) via Toni Rico (@tonidero)
* [Paywalls V2] Adds progress indicator to buttons (#2198) via JayShortway (@JayShortway)
* Avoids triggering "unscheduled" workflows when triggering workflows via the CircleCI API (#2200) via JayShortway (@JayShortway)
* [Paywalls V2] Adds a note on publishing to the missing paywall error. (#2193) via JayShortway (@JayShortway)
* Adds X-Kotlin-Version header. (#2197) via JayShortway (@JayShortway)
* [Paywalls V2] Adds docs on ignored arguments for Paywalls V2 in more places. (#2195) via JayShortway (@JayShortway)
* chore: Add backend integration test for events (#2189) via Facundo Menzella (@facumenzella)
* [Paywalls V2] Adds CI job to update template previews (#2192) via JayShortway (@JayShortway)
