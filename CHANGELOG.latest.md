## RevenueCat SDK
### 🐞 Bugfixes
* Use `Block store` to backup anonymous user ids across installations (#2595) via Toni Rico (@tonidero)

## RevenueCatUI SDK
### Paywallv2
#### 🐞 Bugfixes
* Fixes price formatting discrepancies on Paywalls for `{{ product.price_per_[day|week|month|year] }}` (#2604) via JayShortway (@JayShortway)

### 🔄 Other Changes
* Revert dokka 2 and gradle 9 update (#2618) via Toni Rico (@tonidero)
* Introduce runtime annotations library and add stability annotations for increasing UI performances (#2608) via Jaewoong Eum (@skydoves)
* Override presented offering context paywalls without offering (#2612) via Toni Rico (@tonidero)
* Add APIs for hybrid SDKs to set presentedOfferingContext (#2610) via Toni Rico (@tonidero)
* Bump Baseline Profiles to 1.4.0 and update profiles (#2611) via Jaewoong Eum (@skydoves)
* Migrate deprecated kotlinOptions to compilerOptions (#2607) via Jaewoong Eum (@skydoves)
* [AUTOMATIC][Paywalls V2] Updates paywall-preview-resources submodule (#2613) via RevenueCat Git Bot (@RCGitBot)
* Migrate amazon & debugview modules to KTS (#2327) via Jaewoong Eum (@skydoves)
* Update to Dokka 2.0.0 (#2609) via Toni Rico (@tonidero)
* Add log when restoring purchases finds no purchases with some troubleshooting (#2599) via Toni Rico (@tonidero)
