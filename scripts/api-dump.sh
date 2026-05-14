#!/usr/bin/env bash

exit_code=0

./gradlew metalavaGenerateSignatureDefaultsBc8Release || exit_code=$?
./gradlew metalavaGenerateSignatureCustomEntitlementComputationBc8Release || exit_code=$?
./gradlew metalavaGenerateSignatureDefaultsBc7Release || exit_code=$?
# `:rules-engine` is single-flavor (no billingclient dimension), so its task
# name has no `Bc8` / `Bc7` qualifier and is not picked up by the variant
# names above. Run it explicitly so the committed `rules-engine/api.txt` is
# regenerated and `api-check.sh` can catch any non-internal API leak.
./gradlew :rules-engine:metalavaGenerateSignatureDefaultsRelease || exit_code=$?

exit $exit_code
