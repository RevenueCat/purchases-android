#!/usr/bin/env bash

exit_code=0

./gradlew metalavaGenerateSignatureDefaultsRelease || exit_code=$?
./gradlew metalavaGenerateSignatureCustomEntitlementComputationRelease || exit_code=$?

exit $exit_code