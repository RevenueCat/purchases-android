#!/usr/bin/env bash
#
# Regenerates the committed FlatBuffers Kotlin accessors for the PoC.
#
# The generated code pins the runtime version (Constants.FLATBUFFERS_<version>), so the flatc
# compiler version MUST match the `flatbuffers` version in gradle/libs.versions.toml. Generating
# with a mismatched flatc produces code that will not compile against the pinned runtime.
#
# Usage: ./scripts/generate-flatbuffers.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA="$REPO_ROOT/purchases/src/flatbuffers/schema/products_section.fbs"
OUT_DIR="$REPO_ROOT/purchases/src/main/kotlin"
GEN_DIR="$OUT_DIR/com/revenuecat/purchases/flatbuffers/generated"
EXPECTED_VERSION="$(grep -E '^flatbuffers = ' "$REPO_ROOT/gradle/libs.versions.toml" | sed -E 's/.*"([^"]+)".*/\1/')"

if ! command -v flatc >/dev/null 2>&1; then
  echo "error: flatc not found. Install it (e.g. 'brew install flatbuffers') at version $EXPECTED_VERSION." >&2
  exit 1
fi

ACTUAL_VERSION="$(flatc --version | sed -E 's/flatc version ([0-9.]+).*/\1/')"
if [ "$ACTUAL_VERSION" != "$EXPECTED_VERSION" ]; then
  echo "error: flatc version mismatch. Expected $EXPECTED_VERSION (from libs.versions.toml), found $ACTUAL_VERSION." >&2
  echo "       Generated code pins the runtime version and will not compile if these differ." >&2
  exit 1
fi

echo "Generating Kotlin accessors with flatc $ACTUAL_VERSION ..."
flatc --kotlin -o "$OUT_DIR" "$SCHEMA"

# flatc's Kotlin output emits public top-level classes with no visibility modifiers, which both
# (a) leaks the accessor types into the SDK's public API and (b) fails -Xexplicit-api=strict.
# Marking the top-level classes `internal` fixes both: the types are no longer public API, and
# members of an internal class are exempt from the explicit-API requirement.
for f in "$GEN_DIR"/*.kt; do
  sed -i '' -E 's/^class /internal class /' "$f"
done

echo "Done. Generated internal Kotlin into $GEN_DIR/"
