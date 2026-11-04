## RevenueCat SDK
### ✨ New Features
* Expose configured ad rewards as a preview API (#4533) via Pol Miro (@polmiro)
### 🐞 Bugfixes
* fix: preserve workflow context in custom paywall impressions (#4560) via Álvaro Marco (@amarco90)
### 📦 Dependency Updates
* [RENOVATE] Update dependency revenuecat to v4.7.1 (#4553) via RevenueCat Git Bot (@RCGitBot)

## RevenueCatUI SDK
### Paywallsv2
#### ✨ New Features
* Keep each tab's carousel page when switching tabs (#4481) via Vinh P. Dinh (@vpdn)
#### 🐞 Bugfixes
* ONC-34: Fix tab-button discount package context (#4535) via Facundo Menzella (@facumenzella)
* Fix duplicate paywall image requests and cache retry state (#4529) via Josh Holtz (@joshdholtz)
* Deduplicate shared paywall image prefetches (#4544) via Josh Holtz (@joshdholtz)
* fix(paywalls): Fix scrolling for max-bounded nested paywall stacks (#4531) via Cesar de la Vega (@vegaro)

### 🔄 Other Changes
* feat(paywalls): add internal exit offer handler to PaywallView (#4534) via Cesar de la Vega (@vegaro)
* refactor(paywalls): remove old Compose inset workaround from PaywallDialog (#4563) via Álvaro Brey (@AlvaroBrey)
* build: raise Kotlin language and API version to 2.1 (#4565) via Álvaro Brey (@AlvaroBrey)
* refactor: drop the Billing Client 7 desugaring workaround (#4564) via Álvaro Brey (@AlvaroBrey)
* Update baseline profiles (#4555) via RevenueCat Git Bot (@RCGitBot)
* feat(checkpoints): Add internal error dialog factory for checkpoints (#4537) via Cesar de la Vega (@vegaro)
* Prevent external implementations of ad reward configurations (#4561) via Pol Miro (@polmiro)
* Document public API conventions and review checks (#4478) via Pol Miro (@polmiro)
* Add configured ad reward lookup to both AdMob samples (#4532) via Pol Miro (@polmiro)
* Implement configured ad rewards lookup (#4496) via Pol Miro (@polmiro)
* Define the configured ad rewards API internally (#4492) via Pol Miro (@polmiro)
* build(deps): bump fastlane-plugin-sentry from 2.8.0 to 2.8.1 (#4558) via dependabot[bot] (@dependabot[bot])
* ci: limit the Lock Threads token to `issues: write` (#4557) via Álvaro Brey (@AlvaroBrey)
* ci: lint GitHub Actions workflows with actionlint (#4556) via Álvaro Brey (@AlvaroBrey)
* WFL-800 | Leep the checkpoint trace id when a step can't be served (#4550) via Rosie Watson (@RosieWatson)
* WFL-800 | Send step analytics for all step types in a flow (#4548) via Rosie Watson (@RosieWatson)
* build(deps): bump fastlane from 2.241.1 to 2.241.2 (#4549) via dependabot[bot] (@dependabot[bot])
* Expose subscriber attributes (#4504) via Dave DeLong (@davedelong)
* fix: Explain why a workflow step's offering is not found (#4540) via Cesar de la Vega (@vegaro)
