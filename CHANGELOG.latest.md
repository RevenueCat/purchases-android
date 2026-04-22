## RevenueCat SDK
### ✨ New Features
* feat(ads): add possibility to update fullscreen content callback (#3325) via Peter Porfy (@peterporfy)
* feat(ads): placement override at show time (#3318) via Peter Porfy (@peterporfy)
### 🐞 Bugfixes
* fix(customer-center): prefer active entitlement when decorating subsc… (#3313) via Guilherme (@GuilhermeMota93)
* fix: migrate `await*` coroutines to `suspendCancellableCoroutine` to prevent `IllegalStateException` when callbacks fire more than once. **Behavior change:** `CancellationException` now propagates from public `await*` functions — callers wrapping these in a broad `catch (e: Exception)` should re-throw `CancellationException` to preserve structured concurrency.

## RevenueCatUI SDK
### Paywallv2
#### ✨ New Features
* Add new HeaderComponent (#3290) via Cesar de la Vega (@vegaro)
