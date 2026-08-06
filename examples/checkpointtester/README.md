# Checkpoint Tester

A sample app for exercising the RevenueCat Checkpoints API on Android. It is the Android counterpart to
`Examples/CheckpointTester` in `purchases-ios`.

Each screen represents a way an app can react to a `CheckpointResult`, which is where the differences between
"hard paywall", "soft paywall" and "onboarding" actually live: the SDK reports what happened, and the app decides
whether that gates anything.

## Setup

Add an API key to the repository's `local.properties`:

```properties
CHECKPOINT_TESTER_API_KEY=goog_your_key
```

If it isn't set, the build falls back to `PAYWALL_TESTER_API_KEY_A`. That's usually what you want: this app
deliberately shares `applicationId` (`com.revenuecat.paywall_tester`) with the paywall tester so both work
against the same store project. Note that installing one uninstalls the other.

```bash
./gradlew :examples:checkpointtester:installDebug
```

Checkpoints are `@InternalRevenueCatAPI`, so every call site here needs `@OptIn(InternalRevenueCatAPI::class)`.

## Which experience gets presented

The current resolver (`RandomWorkflowCheckpointResolver`) picks a **random** presentable workflow from remote
config, so the checkpoint identifier does **not** select a specific experience. Only two identifiers behave
deterministically:

| Identifier | Result |
|---|---|
| `unknown_checkpoint` | `CheckpointResult.NoAction` with reason `NO_MATCH`, no UI presented |
| `error_checkpoint` | The call throws a `PurchasesException` with `ConfigurationError` |

Everything else (`hard_paywall`, `soft_paywall`, `onboarding_complete`, `entitlement_gate`) resolves to whatever
workflow the resolver happens to pick, presented as a real workflow paywall by
`CheckpointWorkflowActivity`. The names are the app's labels for its own gating behavior, not a selector.

## Use cases

**Hard paywall** — hits `hard_paywall` on entry. Access is granted only on `Purchased` or `Restored`; a
`Dismissed` outcome leaves the content locked and offers a *Try again* button that re-presents. This is the loop
worth testing.

**Soft paywall** — hits `soft_paywall` on entry, but the content renders unconditionally. The outcome only
changes a banner, showing that a soft paywall never blocks.

**Onboarding** — a three-step flow that hits `onboarding_complete` between the last input step and the final
step. It always advances, whatever the outcome, and shows the result on the final step.

**Entitlement gate** — the closest to a real integration. Reads `CustomerInfo` first and only hits
`entitlement_gate` when nothing is active, then re-reads `CustomerInfo` so a purchase visibly flips the gate.

**No action / Simulated error** — run inline from the use-case list using the two deterministic identifiers above.

**Listener log** — the second tab renders everything the app-wide `CheckpointListener` (registered in
`MainApplication` as `CheckpointEventLog`) observed: an `onCheckpointHit` and an `onCheckpointCompleted` entry per
run, regardless of which screen triggered it. Events are also written to logcat under the `CheckpointEventLog`
tag.

## Structure

- `MainApplication` — configures the SDK and registers the global `CheckpointListener`.
- `checkpoints/CheckpointEventLog` — the `CheckpointListener` implementation backing the listener log.
- `ui/screens/…` — one package per use case, each a screen plus a `ViewModel` holding its state across
  configuration changes.

There is deliberately **no shared helper** wrapping the checkpoints API. Every `ViewModel` calls
`Purchases.sharedInstance.awaitCheckpoint(...)` itself and does its own `when` over `CheckpointResult` and
`CheckpointPaywallOutcome`, so each file shows the whole call in one place — including the `CheckpointParams`
construction and the `PurchasesException` handling. The `when` blocks look similar across screens on purpose:
the branches resolve to different behavior in each use case, and that difference is the thing worth reading.
