# FastBinaryEncoding (FBE) Proof of Concept

Evaluation of [FastBinaryEncoding](https://github.com/chronoxor/FastBinaryEncoding) for
encoding/decoding HTTP-shaped payloads in the SDK.

The generated code lives in **main**, but every top-level type is marked `internal`, so it is not
part of the SDK's public API (Metalava api-check is unaffected) and is not accessible to external
consumers. The whole `fbepoc` package is excluded from detekt (see the `detektAll` task in the root
`build.gradle.kts`) because generated code does not follow the repo's style rules. The PoC is
exercised by `FbeProofOfConceptTest` in the test source set, which can see the `internal` types
because it compiles into the same module.

## Layout

- `poc.fbe` — the schema (source of truth). Generic dummy models: `DummyRequest`, `DummyResponse`,
  `Header`, plus the `HttpMethod` and `ResponseStatus` enums. Exercises primitives, an optional
  field (`string?`), a nested-struct list (`Header[]`), and byte/int32-backed enums.
- `models/` — generated domain objects (`DummyRequest`, etc.) and their FBE field models under
  `models/fbe/`.
- `fbe/` — the shared FBE runtime (`Buffer`, `Model`, per-type `FieldModel*`). Emitted by the
  compiler; identical across schemas for a given FBE version.
- `FbeProofOfConceptTest.kt` — round-trip encode/decode tests + a size-vs-JSON comparison.

`FbeProofOfConceptTest.kt` lives in the test source set
(`purchases/src/test/java/com/revenuecat/purchases/fbepoc/`).

All files under `models/` and `fbe/` are generated. Do not edit by hand; change `poc.fbe` and
regenerate. After regenerating, re-apply the `internal` visibility modifier to every top-level type
(the `fbec` compiler emits them as public):

```bash
# from this directory, after running fbec
find models fbe -name '*.kt' -print0 | xargs -0 sed -i '' -E \
  's/^((open |abstract |sealed |data )*(class|object|interface|enum class) )/internal \1/'
```

## Regenerating

The `fbec` compiler is not published to Maven; it is built from source.

```bash
# 1. Build the compiler (one time)
brew install cmake flex bison
python3 -m pip install --user gil
git clone https://github.com/chronoxor/FastBinaryEncoding.git
cd FastBinaryEncoding && gil update
mkdir -p temp && cd temp
PATH="/opt/homebrew/opt/bison/bin:/opt/homebrew/opt/flex/bin:$PATH" \
  cmake -DCMAKE_BUILD_TYPE=Release -G "Unix Makefiles" ..
PATH="/opt/homebrew/opt/bison/bin:/opt/homebrew/opt/flex/bin:$PATH" make -j4 fbec
# binary at FastBinaryEncoding/temp/fbec

# 2. Generate Kotlin from this schema (run from this directory)
#    --kotlin7 is the Android-friendly variant (avoids java.time, which needs API 26).
/path/to/FastBinaryEncoding/temp/fbec --kotlin7 --input=poc.fbe --output=.
```

The compiler also supports `--json` (Gson-based JSON serialization), `--final` (faster fixed-layout
serialization), and `--proto` (sender/receiver protocol). This PoC uses only the default binary
serialization, which has no third-party dependencies.

## Notes for productionizing

- Types are kept `internal` so they stay out of the tracked public API (`./scripts/api-check.sh`).
  If any need to be exposed externally, that would change the public API signature.
- Generation + the `internal` post-processing should be wired as a Gradle task rather than committed
  by hand if this moves forward.
