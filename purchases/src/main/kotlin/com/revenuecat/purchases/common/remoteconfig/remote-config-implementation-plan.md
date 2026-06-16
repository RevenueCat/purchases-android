# SDK Configuration (`/v2/config`) — SDK Implementation Plan

> Living tracker — check off steps as PRs merge. The SDK-relevant parts of the backend/server spec are folded
> into the [Backend protocol reference](#appendix-backend-protocol-reference-v2config) appendix at the bottom of
> this document.

## Context

The backend is introducing `/v2/config` (see the [Backend protocol reference](#appendix-backend-protocol-reference-v2config)
appendix), a single topic-based configuration endpoint designed to eventually replace many discrete endpoints
(offerings/paywalls/workflows,
`product_entitlement_mapping`, base URLs, etc.). The SDK syncs configuration by topic, sends a local manifest
to avoid re-downloading unchanged data, receives only changed topic bodies, and may receive missing blob
payloads inlined in a binary multipart (`application/x-rc-format`) response.

This plan covers the **first two consuming features** while building a **generic, extensible topic-handler
layer** so future topics are additive:

1. **`sources` topic** — base URLs for API requests and blob APIs.
2. **`product_entitlement_mapping` topic** — product→entitlement mapping for offline entitlements.

### Confirmed scope decisions
- **Base URLs**: resolve + persist the `sources` topic and use its blob URL templates for remote-config blob
  fetching only. **Do not** reroute live API traffic (offerings, customer info, etc.) yet — that is a
  documented future phase.
- **product_entitlement_mapping**: parse the topic blob into the existing `ProductEntitlementMapping` model and
  write it to the existing `DeviceCache` PEM cache, so `OfflineEntitlementsManager`/`PurchasedProductsFetcher`
  consume it unchanged. When the feature flag is off (or remote config is unavailable),
  `/v1/product_entitlement_mapping` remains the source of truth.
- **Single feature flag gates the whole feature**: one flag decides **whether the `/v2/config` request is
  performed at all**, and therefore whether the `sources` and `product_entitlement_mapping` topics are applied.
  Off → no request is made, no topics applied, PEM falls back to `/v1`. On → the sync runs and both topics are
  applied. There is no separate per-topic flag and no separate kill-switch — it is the same flag everywhere.
- **Blobs**: implement inline blob delivery (parse `RCContainer` content elements, verify by hash) **and**
  fetch missing prefetch blobs from the resolved blob sources (verify by hash before caching).
- **Sync cadence**: when the flag is on, sync on **app foreground when stale** only (mirroring the PEM cadence).
  No eager sync on configure.

## Current state of the codebase

Already merged (treat as **baseline / Phase 0**):
- `Endpoint.GetRemoteConfig` → `/v2/config`, `expectsBinaryResponse = true`
  (`common/networking/Endpoint.kt`). Still WIP: TODO comments on final path/fallback.
- `Backend.getRemoteConfig(...)` (`common/Backend.kt`): dispatches the request, parses the binary
  `RCContainer`, supports a debug `BuildConfig.REMOTE_CONFIG_BASE_URL` override. **Currently sends `body = null`
  (no manifest) and does not distinguish `204`.**
- Binary container parsing: `RCContainer` / `RCElement` / `RCContainerFormatException`
  (`common/networking/`). Element 0 = config JSON; elements 1..n = content-addressed blobs keyed by 24-byte
  (192-bit) SHA-256 truncation, exposed as URL-safe base64 (`checksumBase64()`).
- Trusted entitlements: `HTTPClient.verifyBinaryResponse(...)` verifies the config element checksum and the
  `X-Signature` over `config.checksumBytes()`. ETag caching is intentionally bypassed for binary endpoints.

**Stale scaffolding — DELETED in Phase 1** (was pre-spec, modelled an older JSON shape, and was entirely
unreachable from production — never constructed outside its own package/tests). Removed so later phases rebuild
on a clean slate with the new models:
- `remoteconfig/RemoteConfigResponse.kt` (+ test) — old `ApiSource`/`BlobSource`/`Manifest`/`Topic`/`TopicEntry`
  models. Replaced by `ConfigurationResponse.kt`.
- `remoteconfig/RemoteConfigManager.kt` (+ test) — stale orchestration (was the only caller of
  `Backend.getRemoteConfig`). Rebuilt in Phases 2/4.
- `remoteconfig/RemoteConfigDiskCache.kt` (+ test) — write-only, not wired. Rebuilt in Phase 2.
- `remoteconfig/TopicFetcher.kt` (+ test) — blob fetch + SHA-256 verify, but validated **64-char hex** refs; the
  new format uses **32-char base64url (24-byte)** refs (`RCElement.checksumBase64()`). Rebuilt in Phase 3.

**Kept (baseline / generic):**
- `remoteconfig/WeightedSource.kt` (+ `WeightedSourceSelectionTest`) — `selectWeighted()` priority/weight
  selection. Reusable as-is (Phase 5). Now has no implementors in `main` until Phase 5 adds source types.
- `Backend.getRemoteConfig` + `Endpoint.GetRemoteConfig` + `BackendGetRemoteConfigTest` — Phase 0 baseline.
  Temporarily has **no production caller** after Phase 1 (the dead manager was removed); re-consumed in Phase 2.

Reference flow to mirror for the PEM consumer side:
- `offlineentitlements/ProductEntitlementMapping.kt` (`fromJson`/`fromNetwork`/`toJson`, org.json based).
- `caching/DeviceCache.kt` (`cacheProductEntitlementMapping`, `getProductEntitlementMapping`,
  `isProductEntitlementMappingCacheStale`, 25h refresh period).
- `offlineentitlements/OfflineEntitlementsManager.kt` (`updateProductEntitlementMappingCacheIfStale`),
  triggered from `PurchasesOrchestrator.onAppForeground`.

## Target architecture

```
PurchasesOrchestrator.onAppForeground
        │  (feature flag ON?  + stale check on persisted manifest.last_refresh_at)
        │  flag OFF → no request; PEM falls back to /v1
        ▼
RemoteConfigManager  ── orchestrates one sync ──────────────────────────────┐
   │ 1. build request manifest from persisted state                          │
   │ 2. Backend.getRemoteConfig(manifest)  → 204 (no change) | 200 RCContainer│
   │ 3. parse config element → ConfigurationResponse                          │
   │ 4. extract + verify blobs (inline elements, then fetch missing)          │
   │ 5. persist manifest + changed topic bodies + prefetched_blobs            │
   │ 6. dispatch changed/removed topics to handlers (sources first)           │
   ▼                                                                          │
ConfigTopicHandler registry  (Map<topicName, handler>)  ◄────────────────────┘
   ├── SourcesTopicHandler            → RemoteConfigSourceProvider (API + blob sources)
   └── ProductEntitlementMappingTopicHandler → DeviceCache PEM cache (flag-gated)

Supporting pieces:
   RemoteConfigDiskCache   persisted manifest + resolved topic bodies
   BlobStore (TopicFetcher) content-addressed blob cache by base64url ref
   RemoteConfigSourceProvider  resolved weighted blob/api sources (blob used now; api future)
```

### Key new/changed types
- `ConfigurationResponse` (config element 0): `domain`, `subdomains?`, `appUuid?`, `manifest`, `topics`,
  `stateHash`.
- `ConfigManifest`: `domain` (default `app`), `topics: Map<String, String>` (name→compact etag),
  `prefetchBlobs: List<String>`, `prefetchedBlobs: List<String>`, `lastRefreshAt: Long`. Persisted and replayed.
- `ConfigTopic` / `ConfigItem` (item metadata = arbitrary JSON; reserved keys `blob_ref`, `prefetch`).
- `ConfigTopicHandler` interface: `val topicName: String`; `fun handle(items, blobStore)`; `fun clear()`.
- `RemoteConfigSourceProvider`: holds resolved blob source URL templates (and API sources for the future),
  selected via `selectWeighted()`.

Adding a future feature = add a `ConfigTopicHandler`, register it, done. Manifest/etag/blob/persistence
plumbing is generic and topic-agnostic.

---

## Phases (each independently PR-able)
Try to keep PRs small and reviewable. PR descriptions should be very concise.

### Phase 0 — Baseline (DONE)
Binary container parsing, signing/trusted-entitlements, `Endpoint.GetRemoteConfig`, `Backend.getRemoteConfig`
skeleton. 
- [x] `RCContainer`/`RCElement`/`RCContainerFormatException`
- [x] `HTTPClient` binary verification + signing
- [x] `Endpoint.GetRemoteConfig`, `Backend.getRemoteConfig` (body=null, 200-only)

### Phase 1 — Wire-format models for `ConfigurationResponse` (DONE)
Replaced the stale `RemoteConfigResponse` models with the spec shape and deleted all stale pre-spec scaffolding.
No behavior wired. PR: stacked on `support-rc-format-remote-config-request`.
- [x] New models in `remoteconfig/ConfigurationResponse.kt`: `ConfigurationResponse`, `ConfigManifest`,
  `ConfigTopic` (typealias `Map<String, ConfigItem>`), `ConfigItem` (reserved `blob_ref`/`prefetch`).
- [x] Parser: `ConfigurationResponse.parse(bytes: ByteArray)` — lenient `Json { ignoreUnknownKeys = true }`,
  decodes UTF-8 JSON. **Phase 2 calls this with `RCContainer.config.data` bytes** (config element 0).
- [x] Deleted stale files + tests (see "Stale scaffolding — DELETED in Phase 1" above).
- [x] Unit tests `ConfigurationResponseTest`: full first response, changed-topics-only, no-changed-topics
  (204-equivalent body), unknown topic name survives, unknown item key ignored, defaults-when-absent, item
  without `blob_ref`.

**Notes for later phases:**
- `ConfigManifest` is the single type for **both** the request manifest (`{ "manifest": {...} }`) and the
  response's `manifest`. `prefetchedBlobs` (`prefetched_blobs`) is request-only; the server omits it (default
  `emptyList()` handles that). First-run request = `ConfigManifest(domain = "app")`.
- A `204` has **no body** — do not feed an empty body to `ConfigurationResponse.parse` (it requires `domain` +
  `manifest`). Phase 2 must branch on the HTTP status before parsing.
- `ConfigItem` only models `blob_ref` + `prefetch`; any other item metadata is dropped on parse. If a future
  topic needs other item fields, extend `ConfigItem` (kept forward-compatible via `ignoreUnknownKeys`).

### Phase 2 — Manifest persistence, request building, `204` handling
Make the sync stateful: persist the manifest, replay it, and treat `204` as a no-op.
> Note: `RemoteConfigDiskCache` and `RemoteConfigManager` were deleted in Phase 1 — create them fresh here
> against the new models (don't expect existing files).
- [ ] `RemoteConfigDiskCache` (new): read+write persisted `ConfigManifest` + resolved topic bodies (JSON file in
  `noBackupFilesDir/RevenueCat/`). Use `JsonProvider.defaultJson` or a local lenient `Json`.
- [ ] `Backend.getRemoteConfig`: accept a `ConfigManifest`, send it as `{ "manifest": {...} }` request body
  (POST), and surface `204` distinctly from `200` (do not parse an empty binary body as a container). Confirm
  whether the request body needs signing (`postFieldsToSign`); default no per spec.
- [ ] `RemoteConfigManager`: build the request manifest from persisted state (empty manifest with just
  `domain=app` on first run); on `200` store the new server manifest + changed topic bodies; on `204` keep
  cache. Staleness derived from persisted `manifest.last_refresh_at`.
- Files: `Backend.kt`, `remoteconfig/RemoteConfigManager.kt`, `remoteconfig/RemoteConfigDiskCache.kt`, tests.

### Phase 3 — Blob store: inline extraction + verification + fetch
Content-addressed blob cache feeding topic handlers.
- [ ] Update `TopicFetcher`/blob store to **base64url 24-byte** refs (align with `RCElement.checksumBase64()`),
  replacing the 64-char-hex assumption.
- [ ] Extract inlined blob content elements from `RCContainer` (elements 1..n keyed by `checksumBase64()`),
  verify hash == ref, cache by ref, then mark as prefetched.
- [ ] Fetch missing prefetch blobs (`server manifest.prefetch_blobs − local prefetched_blobs`) from the
  resolved blob source URL template (`{blob_ref}` substitution), verify hash, cache.
- [ ] Maintain `prefetched_blobs` (only refs actually present locally) and clean up unreferenced blobs.
- Files: `remoteconfig/TopicFetcher.kt`, `RemoteConfigManager.kt`, tests (verification rejects mismatched bytes).
- Note: bootstrapping resolved by inline blobs on first sync; missing ones use sources resolved in the same
  response (Phase 5), so `sources` must be applied before blob fetch.

### Phase 4 — Topic-handler registry (generic dispatch)
The extensibility seam. Pure plumbing, no feature behavior.
- [ ] `ConfigTopicHandler` interface + a registry (`Map<topicName, handler>`) injected into `RemoteConfigManager`.
- [ ] Dispatch: on `200`, for each **changed** topic invoke its handler with the topic items + blob store; for
  topics removed from `manifest.topics`, drop cached body and call `handler.clear()`; topics present but
  unchanged are left untouched.
- [ ] Deterministic ordering so `sources` is applied before blob-dependent topics.
- [ ] Unknown topic names (no registered handler) are persisted but ignored — forward-compatible.
- Files: new `remoteconfig/ConfigTopicHandler.kt` + registry, `RemoteConfigManager.kt`, tests.

### Phase 5 — `sources` topic handler → `RemoteConfigSourceProvider`
Feature 1. Resolve + persist sources; use blob sources for blob fetching. **No live-API rerouting.**
- [ ] `RemoteConfigSourceProvider`: holds resolved blob source URL templates and API sources (weighted via
  `selectWeighted()`); persisted; survives restarts.
- [ ] `SourcesTopicHandler`: parse the `sources` topic's items (incl. the `blob` item URL template with the app
  blob prefix embedded) into the provider.
- [ ] Wire Phase 3 blob fetching to consult the provider for the blob URL template.
- [ ] Explicitly document that live API base-URL rerouting is **out of scope** (future phase).
- Files: new `remoteconfig/SourcesTopicHandler.kt`, `remoteconfig/RemoteConfigSourceProvider.kt`,
  `TopicFetcher.kt` (use provider), tests.

### Phase 6 — `product_entitlement_mapping` topic handler
Feature 2. Feed the existing offline-entitlements cache from remote config.
- [ ] `ProductEntitlementMappingTopicHandler`: load the topic's default item blob → JSON →
  `ProductEntitlementMapping.fromJson(...)` → `DeviceCache.cacheProductEntitlementMapping(...)`.
- [ ] Add the **single remote-config feature flag** to `AppConfig` (the same flag that gates whether the
  request is performed — see Phase 7). When **on**, remote config populates the existing PEM cache (existing
  consumers unchanged); when **off** or remote config is unavailable, `/v1/product_entitlement_mapping` remains
  the source of truth.
- [ ] Reconcile with `OfflineEntitlementsManager.updateProductEntitlementMappingCacheIfStale` so the two paths
  don't double-fetch (flag-on path skips `/v1`).
- Files: new `remoteconfig/ProductEntitlementMappingTopicHandler.kt`, `OfflineEntitlementsManager.kt`,
  `AppConfig.kt` (flag), tests.

### Phase 7 — Trigger wiring + cadence + flag-gating
Turn it on.
- [ ] Construct `RemoteConfigManager` (+ registry, source provider, disk cache, blob store) in the orchestrator
  dependency graph.
- [ ] **Gate the request on the feature flag**: `RemoteConfigManager.updateRemoteConfigIfNeeded(...)`
  early-returns (no `/v2/config` request) when the flag is off. Only when the flag is on does it proceed to the
  staleness check and perform the sync. This is the same single flag introduced in Phase 6 — it gates the whole
  feature, not just PEM.
- [ ] Call `updateRemoteConfigIfNeeded(appInBackground)` from `PurchasesOrchestrator.onAppForeground` when the
  persisted manifest is stale (foreground + staleness only; no eager sync on configure).
- [ ] Confirm default staleness window.
- Files: `PurchasesOrchestrator.kt`, orchestrator/factory wiring, `RemoteConfigManager.kt`, `AppConfig.kt`, tests.

### Phase 8 — Future (documented, NOT in this effort)
- Live API base-URL rerouting via the `sources` topic (Backend/HTTPClient consult `RemoteConfigSourceProvider`
  with fallback to `AppConfig`).
- `subdomains` support (sync child domains; merge topics across the domain tree; commit-unit semantics).
- Additional topics (offerings/paywalls/workflows) — each a new `ConfigTopicHandler`.
- Finalize the endpoint path/fallback (remove the WIP TODOs in `Endpoint.GetRemoteConfig`).

---

## Critical files
- `common/networking/Endpoint.kt` — `GetRemoteConfig` (finalize path/fallback in Phase 8).
- `common/Backend.kt` — `getRemoteConfig` (manifest body + 204, Phase 2).
- `common/HTTPClient.kt` / `common/networking/RCContainer.kt` / `RCElement.kt` — binary + signing (baseline; blob
  element extraction reads from here in Phase 3).
- `common/remoteconfig/RemoteConfigResponse.kt` — rewrite to spec models (Phase 1).
- `common/remoteconfig/RemoteConfigManager.kt` — orchestration (Phases 2,4).
- `common/remoteconfig/RemoteConfigDiskCache.kt` — manifest/topic persistence (Phase 2).
- `common/remoteconfig/TopicFetcher.kt` — blob store, base64url refs, inline+fetch (Phases 3,5).
- `common/remoteconfig/WeightedSource.kt` — reuse `selectWeighted` (Phase 5).
- `common/offlineentitlements/{ProductEntitlementMapping,OfflineEntitlementsManager}.kt`,
  `common/caching/DeviceCache.kt` — PEM consumer side (Phase 6).
- `PurchasesOrchestrator.kt`, `common/AppConfig.kt` — wiring + flags (Phases 6,7).

## Verification
- **Unit tests** per phase (the primary gate):
  - Phase 1: parse representative `ConfigurationResponse` payloads incl. unknown topics/items.
  - Phase 2: empty/first-run manifest → full sync; replay manifest → `204` no-op; `200` updates persisted state.
  - Phase 3: inline blob extraction + hash verification; reject tampered bytes; fetch-from-source path; cleanup
    of unreferenced blobs.
  - Phase 4: changed topic → handler invoked; removed topic → `clear()`; unchanged topic → untouched; unknown
    topic ignored; `sources` ordered first.
  - Phase 5: `sources` parsed into provider; blob fetch uses resolved template.
  - Phase 6: PEM topic blob → existing PEM cache (flag on); `/v1` fallback (flag off).
  - Phase 7: flag **off** → `updateRemoteConfigIfNeeded` performs no `/v2/config` request (verify no Backend
    call); flag **on** + stale → request performed; flag **on** + fresh → no request (staleness short-circuit).
- **Flavor matrix**: run `:purchases:testDefaultsBc8DebugUnitTest` (and `Bc7`) per
  `purchases-android/CLAUDE.md`.
- **Debug end-to-end**: point `BuildConfig.REMOTE_CONFIG_BASE_URL` (debug override already supported in
  `getRemoteConfig`) at a test backend; foreground the app; assert `200` populates the PEM cache and the source
  provider, and a second foreground replays the manifest and gets `204`.
- **Static checks**: `./gradlew detektAll` and `./scripts/api-check.sh` (new public surface should stay
  `internal`/`@InternalRevenueCatAPI`).

## Open items to confirm during implementation
- Exact `sources` topic item schema (item names, whether multiple weighted blob/api source URLs are provided
  per item) — finalize the `SourcesTopicHandler` parse against a real payload.
- Whether the `/v2/config` request body must be signed (`postFieldsToSign`); default: no.
- Default staleness window for the manifest (reuse PEM's 25h, or backend-driven via `last_refresh_at`).
- Final endpoint path + fallback path (currently WIP in `Endpoint.GetRemoteConfig`).

---

## Appendix: Backend protocol reference (`/v2/config`)

The SDK-relevant subset of the server spec. Server-internal mechanics (DAL persistence, resolver internals) are
omitted; everything a client must implement or rely on is preserved here.

### Overview

SDK Configuration is the server-side system behind `POST /v2/config`. It lets SDKs synchronize app configuration
**by topic**, avoid downloading unchanged data, and optionally receive missing blob payloads inline in a
multipart response. Server-side it has three layers (DAL persistence; topic resolution via `ConfigResolver`;
the HTTP boundary). Only the HTTP boundary and the topic/blob contract below are client-facing.

### Domains

Configuration is partitioned into **domains**. Each domain declares its own topics, and the client-sent
`manifest.domain` selects which domain serves the request (default `app`). Domains let the system scale: topics
can be split out of a domain when they grow too large, and separate config consumers (e.g. a future ads SDK) can
get their own domain.

| Domain | Purpose |
| --- | --- |
| `app` | RC SDK app configuration. |

Requests with an undefined domain fail with `UNKNOWN_CONFIG_DOMAIN` / `400`. Manifests, topic etags, and sync
tokens are all **domain-scoped** — state never leaks between domains.

### Subdomains (future SDK work; client rules captured for later)

A domain can declare **subdomains**: other domains the SDK should also sync to assemble the full configuration.
Subdomains let the server reorganize topics without breaking SDKs — when a topic moves to a dedicated domain,
SDKs that follow subdomains keep receiving it from the new domain.

Subdomain names are returned as a top-level `subdomains` response property (omitted when none):

```json
{ "domain": "app", "subdomains": ["app_workflows"], "...": "..." }
```

A subdomain is an ordinary domain: each is synced with its own `/v2/config` request, manifest, etags, and
cadence. **Client rules:**

1. Persist the latest `subdomains` list alongside the domain manifest. `204` responses have no body, so the
   stored list stays current until a `200` replaces it.
2. Cache topics by `(domain, topic)` and compute merged configuration as the union of topics across the domain
   tree. A topic name is served by at most one domain in the tree.
3. Treat the tree sync as the **commit unit**: after a `200` from a domain, sync new/changed subdomains before
   applying topic deletions and swapping in the merged result. A topic that moved domains then never transiently
   disappears, and a failed child fetch leaves the previous merged config in place.
4. When a domain disappears from its parent's `subdomains` list, drop that domain's manifest and cached topics.
5. Guard recursion with a visited set and a small depth cap (server validates acyclicity, but don't rely on it).

Note: the `subdomains` list is outside the manifest, so it does not participate in manifest etags/change
detection. Moving topics between domains always changes the parent manifest (so synced clients discover the new
subdomain in the same `200`), but adding a subdomain whose topics never lived in the parent does not — synced
clients only discover it when the parent next changes.

### HTTP endpoint

```
POST /v2/config
```

SDK blueprint route, public SDK API key or IAM access token auth. Requires an authorized `app_config`,
otherwise returns `INVALID_PLATFORM`.

**Request body:**

```json
{
  "manifest": {
    "domain": "app",
    "topics": { "sources": "Jc83RzcK1LqA", "paywalls": "pUe5xcmVf0pQ" },
    "prefetch_blobs": ["base64url-sha256"],
    "prefetched_blobs": ["base64url-sha256"],
    "last_refresh_at": 1710000000
  }
}
```

The server does **not** trust client-supplied location: `storefront` is read from the `X-Storefront` header, and
`geoip` (country, region, city, postal code, time zone, continent) is derived from the request's best-guess
client IP. (The SDK already sends `X-Storefront`.)

**JSON response body (fallback shape; also the config element of the binary response):**

```jsonc
{
  "domain": "app",
  "subdomains": ["app_workflows"],   // domains to also sync; omitted when none
  "app_uuid": "1a2b3c4d",
  "manifest": {
    "domain": "app",
    "topics": { "sources": "Jc83RzcK1LqA", "paywalls": "9v1DnUu6rXbE" },
    "prefetch_blobs": ["base64url-sha256"],
    "last_refresh_at": 1710000100
  },
  "topics": {
    "paywalls": {              // <-- TopicName; only CHANGED topics included
      "default": {             // <-- ItemName
        "blob_ref": "base64url-sha256",
        "prefetch": true
      }
    }
  },
  "state_hash": "x3R7YvQw2NfM"
}
```

- The response `topics` map contains **only changed topic bodies**. A topic whose client-sent etag still matches
  is omitted; the client keeps its locally cached topic data. `manifest.topics` still lists **every** active
  topic and its current etag, including unchanged ones.
- Each topic body is a map of `ItemName` → item metadata (arbitrary JSON). `blob_ref` and `prefetch` are
  reserved conventions: `blob_ref` means the item has an external static blob payload to fetch; `prefetch: true`
  means the SDK should proactively cache that blob.
- `manifest.prefetch_blobs` lists all blob refs the server believes the SDK should have prefetched for the
  resolved configuration — not limited to changed topics.

### Local manifest fields

The SDK keeps a local manifest and sends it back on each request:

| Field | Meaning |
| --- | --- |
| `domain` | Configuration domain the manifest belongs to. Defaults to `app`. |
| `topics` | Topic name → compact topic etag. |
| `prefetch_blobs` | Blob refs the previous server manifest requested for prefetch. |
| `prefetched_blobs` | Blob refs the SDK has actually cached locally. |
| `last_refresh_at` | Timestamp from the previous server manifest. Used only for refresh cadence. |

**From scratch**, a client with no stored manifest sends just the domain:

```json
{ "manifest": { "domain": "app" } }
```

Empty topics and a zero `last_refresh_at` make the server resolve everything and return the full configuration
plus a complete manifest, which the client stores and replays from then on.

### Synchronization flow

**Server flow (for context):** select resolver for `manifest.domain` (unknown → `UNKNOWN_CONFIG_DOMAIN`/`400`);
build manifest state; if no resolve needed, return `204` with `X-RevenueCat-ETag` and empty body; otherwise
resolve each topic, compute per-topic etags, build a fresh server manifest, and return `204` when topics +
prefetch refs match with no missing prefetch blobs, else `200` with the full manifest and only changed topic
bodies.

**Client-side incremental flow (what the SDK must implement):**

1. Store the latest server manifest and send it back as `manifest` on the next request.
2. Cache topic data independently by topic name.
3. On a `200`, when `topics` includes a topic, **replace** the cached data for that topic.
4. On a `200`, when a topic is omitted but still present in `manifest.topics`, **keep** the cached data (etag
   still matches).
5. When a previously cached topic is **no longer present** in `manifest.topics`, **delete** the cached topic
   data (server removed it for this client).
6. Cache blob payloads by blob ref. When the response inlines blobs, scan the inlined blob elements and store
   each payload under its ref **after verifying** the payload bytes hash to a blob ref referenced by the
   configuration. The element checksum key is an untrusted lookup hint (see Response signing).
7. On the next request, send `prefetched_blobs` for blobs actually present in the local blob cache.

### Blob storage and blob refs

The `sources` topic tells SDKs where API responses and blob payloads are fetched from. Its `blob` item contains
URL templates with the app blob prefix already embedded; clients substitute `{blob_ref}` with the 32-char
base64url blob ref. (Blob refs are the URL-safe base64 of a 24-byte / 192-bit SHA-256 truncation — consistent
with `RCElement.checksumBase64()`.)

### Blob inlining (binary format)

Responses can inline missing prefetched blobs to reduce round trips. The SDK opts in with the
`Accept: application/x-rc-format` header (RevenueCat binary multipart format). The server also supports
`multipart/mixed` (MIME) for other clients, but **the Android SDK does not use it** — it always requests and
parses the binary format via `RCContainer`/`RCElement`. If the client does not accept a multipart type, the
server falls back to plain JSON (only the config body, no inlined blobs).

Inlining behavior:

1. The server resolves configuration exactly like the JSON path.
2. It computes `missing_prefetch_blobs = server_manifest.prefetch_blobs − client_manifest.prefetched_blobs`.
3. If the binary format is accepted, it inlines those blobs as container elements.
4. The leading element (element 0) is the JSON-serialized config-response bytes.
5. Each missing blob is added as an element keyed by the blob ref as raw 24 bytes (binary, no text encoding) —
   exactly what `RCContainer`/`RCElement` already parse. Inlining stops once payloads reach a soft byte cap
   (3 MB default, server-configurable); blobs left out remain in `manifest.prefetch_blobs`, so the client
   fetches them from the blob sources instead.

### Response signing

The endpoint uses the standard signed-response path: `X-Signature` (Ed25519) covers the request context (api
key, nonce, path, request time), the `X-RevenueCat-ETag` value, and the payload.

| Response | Signed payload |
| --- | --- |
| JSON `200` | The full JSON body. |
| Binary `200` | Only the leading config element. |
| `204` | Empty; the signature still covers `X-RevenueCat-ETag`, so the sync token is tamper-evident. |

Inlined blob elements are intentionally **not** signed. Blobs are authenticated transitively through content
addressing: the signed config references each blob by its SHA-256 `blob_ref`, so verifying a blob means hashing
its bytes and comparing against a ref taken from the signed config. One trust chain
(`signature → config → blob_ref → blob bytes`) whether a blob arrives inline or from the blob sources, and the
body can stream without buffering to sign.

**Client verification rules:**

1. Verify `X-Signature` over the config element bytes exactly as received (the full body for JSON). The SDK
   already does this via `HTTPClient.verifyBinaryResponse(...)` over `config.checksumBytes()`.
2. Verify **every** blob payload — inline or fetched from blob sources — by hashing it against a `blob_ref` from
   the signed config before caching it. The element checksum keys are untrusted lookup hints only.
