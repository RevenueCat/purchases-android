# Mission: consolidate and align the Paywalls V2 `web_view` component (NOT a rewrite)

**Audience:** an autonomous coding agent working in this repository (`RevenueCat/purchases-android`), starting from `main`.
**Goal:** one clean feature branch off `main` carrying the existing (unmerged) `web_view` implementation plus an enumerated set of alignment and hardening fixes, so Android matches the cross-platform contract decisions finalized during the iOS rebuild.
**You are done when every box in [Definition of done](#definition-of-done) is checked and the [verification loop](#verification-loop) is green.**

---

## 1. Why this is NOT a rewrite (read before changing anything)

The iOS `web_view` prototype was rebuilt from scratch because its architecture was over-decomposed (~8 collaborating types for one session, duplicated platform views, three stacked sizing mechanisms). **Android does not have that problem.** The existing Android implementation already has the shape the iOS rebuild was aiming for:

- One owner for all transport state: `WebViewJavaScriptBridge` (handshake, origin checks, threading, auto-reply, resize) — this class was the *reference implementation* for the iOS rebuild.
- One view: `WebViewComponentView` (Compose `AndroidView`), no duplication.
- Inbound `kind` filtering is already correct (`connect` / `message`+`request` / else-drop).
- Both `fit` axes are already declared **and applied** (`contentWidthCssPx`/`contentHeightCssPx` → `effectiveSize`), so the declare-iff-apply invariant holds.
- Public API (`PaywallWebViewMessage`/`Value`/`Controller`/`MessageHandler`, `PaywallOptions`/`PaywallDialogOptions` wiring) is settled and tested.

**Therefore: reuse the existing code verbatim. Your job is consolidation + the specific fixes in §4. Do not restructure classes, rename files, or "simplify" beyond the tasks listed.** If you find yourself rewriting `WebViewJavaScriptBridge` or `WebViewComponentView` wholesale, you have misread this mission.

## 2. Ground rules

1. **Do NOT push to, rebase, or force-push any existing branch** (`alexrepty/paywalls-web-view-*`, `alvaro/paywall-tester-web-view-sample`, `cursor/webview-hardening-3e00`). They belong to other engineers' work. You work only on your own new branch.
2. **Code is the source of truth.** Several stack PR bodies (#3652, #3654, #3655) describe older protocol revisions — ignore them where they conflict with code.
3. **Do not change:** the wire protocol names (`rcWebComponents`, `__rcWebComponentsReceive`, channel `rc-web-components`, kinds, `rc:*` types), the public API names, or the message limits (64 KiB, depth 16).
4. **Do not add:** purchase/restore/dismiss message types; dashboard custom variables over the bridge; URL variable substitution (removed deliberately — URLs are static); bundle caching; feature flags; CSP header injection via `shouldInterceptRequest` (explicitly out of scope; the meta-injection race stays documented-only).
5. One commit per task, push early and often; open ONE draft PR.

## 3. Setup — consolidation

The full feature currently lives on an unmerged Graphite stack (#3650→#3653) with a hardening branch on top. The branch `cursor/webview-hardening-3e00` is stacked on the whole thing, so merging it brings the complete feature:

```bash
cd <repo-root-of-purchases-android>
git fetch origin main cursor/webview-hardening-3e00
git checkout -b <your-branch-name> origin/main
git merge origin/cursor/webview-hardening-3e00   # brings the entire stack + hardening fixes
```

Resolve any conflicts against `main` conservatively (component-registration files like `PaywallComponentSerializer`, `StyleFactory`, `ComponentView` gain entries from many PRs — keep both sides). Then apply the §4 tasks as separate commits on top. Your PR (base `main`) supersedes the stack; note that in the body but do not close anyone's PRs yourself.

Key files (all under `ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/` unless noted): `components/webview/WebViewJavaScriptBridge.kt`, `WebViewEnvelope.kt`, `WebViewMessageParser.kt`, `WebViewComponentView.kt`, `WebViewUrlResolver.kt`, `WebViewContentSecurityPolicy.kt`, `PaywallWebViewVariablesProvider.kt`; `PaywallWebViewMessage.kt`, `PaywallWebViewValue.kt` (module root); schema in `purchases/src/main/kotlin/com/revenuecat/purchases/paywalls/components/WebViewComponent.kt`. Tests in `ui/revenuecatui/src/test/kotlin/.../components/webview/`.

## 4. Tasks, in order

These encode cross-platform decisions already made during the iOS rebuild (see `Contributing/WebViewComponentRebuild.md` on the `purchases-ios` branch `cursor/web-view-rebuild-plan-6dd6` for full rationale). They are decisions, not suggestions.

### Task 1 (P0) — Host protocol version is the hard-coded constant 1

`WebViewComponentView` currently constructs the bridge with `protocolVersion = style.protocolVersion ?: WebViewEnvelope.DEFAULT_PROTOCOL_VERSION`, i.e. the schema value becomes the host's advertised capability. Wrong: the envelope version is the wire+SDK major the *content* speaks, and this SDK build implements exactly v1 — a schema declaring `2` must NOT make the host accept a v2 handshake it cannot service.

- Pass `WebViewEnvelope.DEFAULT_PROTOCOL_VERSION` to the bridge unconditionally. Keep decoding/preserving `style.protocolVersion` (it may gate other behavior later), but it must not feed the handshake or outbound envelopes.
- Reject error string stays: `Unsupported protocol_version N; native host supports 1`.
- Tests: connect with version 2 → reject naming 1, even when the style's `protocolVersion` is 2.

### Task 2 (P0) — Pre-parse structural depth scan

`WebViewEnvelope.parse` calls `JSONObject(rawJson)` directly; org.json's tokenizer recurses per nesting level, and tens of thousands of levels fit inside the 64 KiB limit — a hostile bundle can stack-overflow the process before `MAX_NESTING_DEPTH` (enforced later, during `PaywallWebViewValue.fromJson` conversion) is ever consulted. Same bug class the iOS rebuild fixed.

- Add a non-recursive byte/char scan (brace/bracket counting outside string literals, honoring `\` escapes) with budget `MAX_NESTING_DEPTH + 1` (envelope wrapper + payload tree), run in `WebViewJavaScriptBridge.postMessage`'s inbound path (or at the top of `WebViewEnvelope.parse`) BEFORE any `JSONObject` construction. Reject → drop with the existing "not a valid transport envelope" log.
- Keep the existing conversion-time depth check as defense in depth.
- Tests: depth `MAX_NESTING_DEPTH` payload accepted; one deeper rejected; a ~30k-deep frame under 64 KiB rejected without crashing (this is the regression test for the overflow).

### Task 3 (P0) — Resize hardening (clamp, threshold, kind-agnostic interception)

Current gaps vs the agreed sizing contract: no 10,000 px clamp, no 1 pt/px apply-threshold, and `handleAppFrame` intercepts `resize` only for `KIND_MESSAGE` — audit what a `KIND_REQUEST` resize does today (if `WebViewMessageParser` drops unknown types it merely times out on the content side; if it forwards, an SDK-internal message leaks to the app handler).

- Intercept `type == RESIZE` for both `message` and `request` kinds, never forwarding it to the app handler.
- Per axis: require finite and > 0; clamp to 10_000; ignore a change smaller than 1 against the last applied value (guards report/apply feedback loops — HTML content's reported width often just echoes the imposed viewport width).
- Placeholder parity with iOS/web: before the first valid report, a `fit` height renders at the platform equivalent of 100 (dp) and a `fit` width at 300 (dp) — audit `computeEffectiveSize` for what a `0` content value currently produces and align, keeping the constants named with a comment giving their origins (100 = iOS prototype, 300 = web `FIT_FALLBACK_SIZE_PX`).
- Tests: clamp on either axis; threshold (re-report within 1 does not update state, ≥ 1 does); request-kind resize not forwarded; width report on a non-fit width ignored; invalid width + valid height still applies the height.

### Task 4 (P0) — Non-finite `PaywallWebViewValue.Number` normalization

Decision (matches iOS; supersedes the open bugbot finding about `-0.0`/`NaN` equals/hashCode): `Number` construction **normalizes non-finite values to `Null`** — JSON cannot represent them, org.json's `put(double)` throws for them, and a throwing serialization would kill an entire otherwise-valid outbound message over one bad value.

- Since `Number` is a public class with public constructors, normalize at the serialization boundary AND make the factory path total: have `toJsonRepresentation()`/`toJsonObject()` emit JSON `null` for non-finite values (never throw), and fix `equals`/`hashCode` to be mutually consistent for `NaN`/`-0.0` (use `value.toRawBits()`-based or `compareTo`-consistent semantics and document them). If a cheap normalization point exists at construction without breaking the public constructor contract, prefer it and document.
- Tests: `Number(Double.NaN)`/`Number(Double.POSITIVE_INFINITY)` serialize as null without throwing inside an otherwise-valid map; equals/hashCode consistency for `NaN` vs `NaN` and `-0.0` vs `0.0` (pin whichever semantics you implement).

### Task 5 (P0) — Same-origin main-frame navigation policy

`shouldOverrideUrlLoading` currently blocks only non-HTTPS navigation; the page can still navigate cross-origin over HTTPS (the bridge origin check kills messaging, but the surface shouldn't move at all). iOS now denies cross-origin main-frame navigation at the source.

- In the `WebViewClient`, deny main-frame navigation whose origin (scheme + lowercased host + normalized port, reusing `toOriginOrNull`) differs from the resolved component URL's origin. Same-origin different-path navigation stays allowed. Sub-frame requests keep current behavior (CSP governs subresources).
- Tests: extend the existing client tests — same-origin path allowed, cross-origin https denied, non-https still denied.

### Task 6 (P1) — Single-parse cleanup

`handleAppFrame` parses `rawJson` twice (`WebViewEnvelope.parse`, then `WebViewMessageParser.parse(rawJson)`). Change `WebViewMessageParser.parse` to accept the already-parsed envelope. No behavior change; existing parser tests updated mechanically. (This is the one small structural cleanup permitted — nothing further.)

### Task 7 (P1) — Schema `fallback` typing check

`WebViewComponent.fallback` is typed `StackComponent?`. Verify against the merged khepri schema (khepri PR #21860 and the base component model) whether the generic component `fallback` for `web_view` can be an arbitrary component rather than a stack. If arbitrary: widen the type to match what the serializer's generic fallback path produces, mirroring how other components type their fallback. If stack-only: add a comment citing the khepri model and move on.

### Task 8 (P2) — Hygiene

- Update stale KDoc left over from earlier protocol revisions anywhere `rg -n "RevenueCatWebView|__revenueCatReceiveMessage|variables key" ui/` still matches.
- PR body: note this branch supersedes the stack PRs (#3650–#3653, #3729) pending team sign-off; apply the `skip-pr-lines-changed-check` label with a one-line justification (consolidation branch).
- CHANGELOG untouched (unreleased feature).

## 5. Definition of done

**Functional / contract**
- [ ] Handshake rejects any content version ≠ 1 regardless of the schema's `protocol_version` (test-proven).
- [ ] A ≤ 64 KiB, ~30k-deep hostile frame is rejected without a crash (test-proven).
- [ ] Resize: clamp 10_000, 1-unit threshold, kind-agnostic interception, fit placeholders 100/300, all test-proven.
- [ ] Non-finite numbers can never throw during outbound serialization; equals/hashCode consistent (test-proven).
- [ ] Cross-origin main-frame navigation denied (test-proven).
- [ ] All pre-existing webview tests still pass unmodified except where a task explicitly changes behavior.

**Repository health**
- [ ] `./gradlew :ui:revenuecatui:detekt` clean; the module's debug unit-test task green (discover the exact task via `./gradlew :ui:revenuecatui:tasks | grep -i test`).
- [ ] API signature file regenerated if any public API changed (`./gradlew tasks --all | grep -i -E "metalava|apiDump|updateApi"`).
- [ ] ONE draft PR, base `main`, body mapping each task to its commit and naming the superseded PRs. Do not close other people's PRs.

**Restraint (the mission)**
- [ ] `WebViewJavaScriptBridge` and `WebViewComponentView` keep their names, files, and overall structure (diff shows targeted edits, not restructuring).
- [ ] No new types beyond what the tasks require.

## 6. Verification loop

Repeat until green:

1. `./gradlew :ui:revenuecatui:detekt` and the module unit-test task; fix and re-run until zero failures. If the environment cannot run Gradle (no Android SDK), say so explicitly in the PR body and use CI: push, then `gh pr checks <PR> --repo RevenueCat/purchases-android --watch`, reading failures via the CircleCI links, fix, push, repeat.
2. Exit condition: all required checks green except failures you can demonstrate exist on `main` (name them) and manual approval holds (leave for humans).
3. Getting-stuck protocol: same failure three times → re-read the task and the iOS counterpart (`purchases-ios` branch `cursor/web-view-rebuild-plan-6dd6`, files `WebViewSession.swift`/`WebViewEnvelope.swift`) before a fourth attempt; five times → write the blocker into the PR body with your analysis instead of thrashing.
