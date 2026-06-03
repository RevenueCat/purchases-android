# FlatBuffers PoC

Proof of concept for passing a discrete **section** of a backend HTTP response to the SDK as a
[FlatBuffer](https://flatbuffers.dev/) instead of JSON. This is an evaluation spike, not a wired
feature: nothing in the production request/response path is changed, and the parser is exercised
only by unit tests.

## Data flow

```
Backend                                   SDK
-------                                   ---
build FlatBuffer (FlatBufferBuilder)      HTTPResult.body : JSONObject   (existing, unchanged)
  -> base64 encode                          -> read "products_section_fb" field
  -> put in JSON field "products_section_fb"  -> base64 decode
                                              -> ProductsSection.getRootAsProductsSection(bytes)
JSON: { ..., "products_section_fb": "<b64>" } -> map to ProductsSectionData (plain Kotlin)
```

The FlatBuffer rides as a base64 string inside the normal JSON body, so the transport
(`HTTPClient`, `HTTPResult`, the ETag cache) needs no changes. The SDK reads one extra field from
the already-parsed `JSONObject` and decodes it.

## Files

- `schema/products_section.fbs` — the mock schema (a products section: list of products + a
  fetched-at timestamp; exercises a table, a vector, scalars, strings, and an enum).
- `../main/kotlin/com/revenuecat/purchases/flatbuffers/generated/` — Kotlin accessors generated
  by `flatc` (committed; do not hand-edit). flatc emits **public** top-level classes with no
  visibility modifiers, which would both leak these types into the SDK's public API and fail the
  module's `-Xexplicit-api=strict`. The regen script post-processes the top-level classes to
  `internal`, which fixes both (the types are no longer public API, and members of an `internal`
  class are exempt from the explicit-API requirement).
- `../main/kotlin/com/revenuecat/purchases/flatbuffers/FlatBuffersProductsSectionParser.kt` — the
  SDK-side parser. Decodes the section and returns the domain model, or `null` on any error
  (same swallow-and-log behavior as `OfferingParser`).
- `../main/kotlin/com/revenuecat/purchases/flatbuffers/ProductsSectionData.kt` — plain Kotlin
  domain models. Generated types never leak past the parser.
- `../test/java/com/revenuecat/purchases/flatbuffers/` — the encoder fixture (the "backend side")
  and a roundtrip test.

## Regenerating the accessors

```bash
./scripts/generate-flatbuffers.sh
```

**Version match is mandatory.** Generated code pins the runtime version via
`Constants.FLATBUFFERS_<version>` (asserted in the generated `ValidateVersion`). The `flatc`
compiler version must equal the `flatbuffers`
version in `gradle/libs.versions.toml` (currently `25.2.10`), otherwise the generated Kotlin will
not compile against the runtime dependency. The script enforces this. Note that Homebrew may ship
a newer `flatc` than the latest published `com.google.flatbuffers:flatbuffers-java` on Maven
Central; pin both to the same released version.

## JSON vs FlatBuffers (evaluation notes)

- **Zero-copy reads:** fields are read directly from the byte buffer with no intermediate object
  graph, unlike `JSONObject`/`kotlinx.serialization` which allocate during parse.
- **Size:** binary is typically smaller on the wire than equivalent JSON (no field-name
  repetition), though base64 embedding adds ~33% back; a fully binary response body would avoid
  that (see below).
- **Schema evolution:** new fields can be appended to a table and old readers ignore them; this
  matches our existing tolerance for unknown JSON keys.
- **Cost:** a `.fbs` schema plus a codegen step, and the version-match constraint above.

## Not done here (follow-ups)

- Wiring into a real endpoint (e.g. a section of the offerings response in `Backend.kt`).
- A **fully binary response body** (binary `Content-Type` instead of a base64 JSON field). That
  is more efficient but more invasive: `HTTPResult.payload` is a `String` today, so it would
  require retaining raw bytes through `HTTPClient` and the ETag cache.
