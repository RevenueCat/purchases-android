## RevenueCat SDK
### ✨ New Features
* Unified StoreReplacementMode API (#3234) via Will Taylor (@fire-at-will)
* Add placement and targeting context to paywall events (#3253) via Dan Pannasch (@dpannasch)
### 🐞 Bugfixes
* Fix null Placements when offering_ids_by_placement is absent (#3254) via Dan Pannasch (@dpannasch)

## RevenueCatUI SDK
### 🐞 Bugfixes
* `RevenueCatUI` Paywalls and Customer Center can now opt in to deriving layout direction from `overridePreferredUILocale` by passing `honorLayoutDirection = true`. Paywalls V2 can also use the dashboard `layout_direction` setting to match locale or force RTL/LTR.

### Paywallv2
#### ✨ New Features
* Wire multipage workflow navigation into PaywallViewModel (#3381) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* Add `triggerType` to `WorkflowTrigger` (#3393) via Cesar de la Vega (@vegaro)
* Extract private function `NavigateTo.toPaywallAction` (#3392) via Cesar de la Vega (@vegaro)
* Bump revenucatui-tests gradle cache key (#3391) via Toni Rico (@tonidero)
* Create `WorkflowTriggerType` and `WorkflowTriggerActionType` (#3386) via Cesar de la Vega (@vegaro)
* Update baseline profiles (#3390) via RevenueCat Git Bot (@RCGitBot)
* Plumb `componentId` through buttons on workflow interactions (#3380) via Cesar de la Vega (@vegaro)
* Add `ButtonComponent.Action.Workflow` (#3385) via Cesar de la Vega (@vegaro)
* Add `componentId` to `ButtonCoomponentStyle` (#3384) via Cesar de la Vega (@vegaro)
* Migrate all suspendCoroutine usages to suspendCancellableCoroutine (#3365) via Jaewoong Eum (@skydoves)
* Add `WorkflowNavigator` for multipage workflow step navigation (#3379) via Cesar de la Vega (@vegaro)
* build(deps): bump fastlane-plugin-revenuecat_internal from `b822f01` to `d24ab26` (#3383) via dependabot[bot] (@dependabot[bot])
* Add `id` field to `ButtonComponent` (#3377) via Cesar de la Vega (@vegaro)
* Add CI workflows for generating Baseline Profiles (#3372) via Jaewoong Eum (@skydoves)
* add min sdk level for paywalls and customer center (#2465) via Muhammad-Sharif Moustafa (@mshmoustafa)
