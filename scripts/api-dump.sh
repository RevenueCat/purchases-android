#!/usr/bin/env bash

exit_code=0

./gradlew metalavaGenerateSignatureDefaultsBc8Release || exit_code=$?
./gradlew metalavaGenerateSignatureCustomEntitlementComputationBc8Release || exit_code=$?
./gradlew metalavaGenerateSignatureDefaultsBc7Release || exit_code=$?
# `:rules-engine` has no Billing Client flavors, so its only signature task is
# the bare `metalavaGenerateSignatureDefaultsRelease`. Invoke it explicitly so
# CI catches drift in `rules-engine/api.txt`.
./gradlew :rules-engine:metalavaGenerateSignatureDefaultsRelease || exit_code=$?

exit $exit_code
