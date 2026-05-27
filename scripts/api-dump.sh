#!/usr/bin/env bash

exit_code=0

# Note: `:rules-engine-internal` is SDK-internal by design and opts out of
# Metalava entirely (see `ConfigureMetalava.kt`), so it has no signature
# generation task to invoke from this script.

./gradlew metalavaGenerateSignatureDefaultsBc8Release || exit_code=$?
./gradlew metalavaGenerateSignatureCustomEntitlementComputationBc8Release || exit_code=$?
./gradlew metalavaGenerateSignatureDefaultsBc7Release || exit_code=$?

exit $exit_code
