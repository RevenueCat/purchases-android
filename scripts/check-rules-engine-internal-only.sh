#!/usr/bin/env bash
#
# Asserts :rules-engine has no public API outside @InternalRulesEngineAPI.
#
# Regenerates rules-engine/api.txt with Metalava (which already hides anything
# annotated @InternalRulesEngineAPI) and then verifies the only declaration
# left in api.txt is the @InternalRulesEngineAPI annotation itself. Anything
# else is a leaked, non-internal public API and fails CI.
#
# This check is intrinsic — it does not rely on diffing against a committed
# baseline, so it cannot be silenced by regenerating api.txt.

set -euo pipefail

API_FILE="rules-engine/api.txt"

./gradlew :rules-engine:metalavaGenerateSignatureDefaultsRelease --no-daemon

remainder=$(awk '
  /^\/\/ Signature format/                                 { next }
  /^package com\.revenuecat\.purchases\.rules \{/          { in_pkg=1; next }
  in_pkg && /^\}/                                          { in_pkg=0; next }
  in_pkg && /public @interface InternalRulesEngineAPI \{/  { in_iface=1; next }
  in_iface && /^[[:space:]]*\}/                            { in_iface=0; next }
  in_iface                                                 { next }
  /^[[:space:]]*$/                                         { next }
  { print }
' "$API_FILE")

if [ -n "$remainder" ]; then
  echo "ERROR: :rules-engine has public APIs not gated by @InternalRulesEngineAPI."
  echo
  echo "Offending content in $API_FILE:"
  echo "$remainder"
  echo
  echo "Every public declaration in :rules-engine MUST be annotated with @InternalRulesEngineAPI."
  exit 1
fi

echo "OK: :rules-engine has no public APIs outside @InternalRulesEngineAPI."
