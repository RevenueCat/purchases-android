## RevenueCat SDK
### 🐞 Bugfixes
* fix: send X-Is-Sandbox header when using a Test Store API key (#4181) via Álvaro Brey (@AlvaroBrey)
* fix: floating-point rounding bug in Test Store priceString (#4164) via Will Taylor (@fire-at-will)

## RevenueCatUI SDK
### ✨ New Features
* feat(paywalls): Add onInteraction to PaywallListener (#4160) via Álvaro Brey (@AlvaroBrey)
### Paywallsv2
#### ✨ New Features
* Add window size conditions for paywall component overrides (#4193) via Josh Holtz (@joshdholtz)
* Feat(Paywalls) Support Underlined Text (#2928) via Jacob Rakidzich (@JZDesign)
#### 🐞 Bugfixes
* fix: Apply the video source to the view when the appearance changes (#4159) via Facundo Menzella (@facumenzella)
### Customer Center
#### ✨ New Features
* feat(Customer Center): purchase history  (#3997) via Facundo Menzella (@facumenzella)

### 🔄 Other Changes
* Remove the CheckpointListener API (#4204) via Toni Rico (@tonidero)
* other: echo step experiment params on workflow events (#4197) via Facundo Menzella (@facumenzella)
* Hoist shadow path and paint out of the draw phase (#4195) via Jaewoong Eum (@skydoves)
* Memoize Markdown parsing and paragraph text building (#4194) via Jaewoong Eum (@skydoves)
* build(deps): bump fastlane from 2.238.0 to 2.239.0 (#4196) via dependabot[bot] (@dependabot[bot])
* feat: send the checkpoint outcome on checkpoint_hit (#4150) via Cesar de la Vega (@vegaro)
* Log checkpoint rule evaluation (#4187) via Toni Rico (@tonidero)
* Update baseline profiles (#4190) via RevenueCat Git Bot (@RCGitBot)
* Chore(Paywalls): Constraints and allocations to support min/max sizes (#4171) via Jacob Rakidzich (@JZDesign)
* Treat a rule on an unsupplied dimension as a non-match (#4177) via Toni Rico (@tonidero)
* Fail rule resolution when the customer changes mid-snapshot (#4135) via Toni Rico (@tonidero)
* Remove backend predicate results from audience evaluation (#4174) via Toni Rico (@tonidero)
* Cache the backend's subscriber dimensions and evaluate rules against them (#4133) via Toni Rico (@tonidero)
* Add the customer's purchases and entitlements as rule evaluation properties (#4130) via Toni Rico (@tonidero)
* Reshape rule evaluation properties (#4126) via Toni Rico (@tonidero)
* Read the reshaped audiences topic: static default blob + backend predicate results (#4124) via Toni Rico (@tonidero)
* ci: bump external PR notifications workflow to v8 (#4176) via Álvaro Brey (@AlvaroBrey)
* ci: notify external PRs feed on PRs from outside the org (#4170) via Álvaro Brey (@AlvaroBrey)
* refactor(checkpoints): Present checkpoints in a window instead of an activity (#4087) via Toni Rico (@tonidero)
* Chore(Paywalls): Update models to support min/max sizes (#4163) via Jacob Rakidzich (@JZDesign)
* [EXTERNAL] Expose a new `DangerousSettings#forceAllowTestStoreInReleaseBuilds ` (#4131) via @cyrilmottier (#4169) via Toni Rico (@tonidero)
