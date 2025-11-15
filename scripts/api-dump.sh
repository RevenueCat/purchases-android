#!/usr/bin/env bash

exit_code=0

./gradlew metalavaGenerateSignatureDefaultsBc8Release || exit_code=$?
./gradlew metalavaGenerateSignatureCustomEntitlementComputationBc8Release || exit_code=$?
./gradlew metalavaGenerateSignatureDefaultsBc7Release || exit_code=$?

exit $exit_code
