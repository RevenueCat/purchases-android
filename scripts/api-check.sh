#!/usr/bin/env bash

exit_code=0

./gradlew metalavaCheckCompatibilityDefaultsRelease || exit_code=$?
./gradlew metalavaCheckCompatibilityCustomEntitlementComputationRelease || exit_code=$?

exit $exit_code