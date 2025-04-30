#!/usr/bin/env bash

echo "Running detekt check..."
OUTPUT="/tmp/detekt-$(date +%s)"
./gradlew detektAll > "$OUTPUT"
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  cat "$OUTPUT"
  rm "$OUTPUT"
  echo "***********************************************"
  echo "                 Detekt failed                 "
  echo " Please fix the above issues before committing "
  echo "***********************************************"
  exit $EXIT_CODE
fi
rm "$OUTPUT"

echo "Running metalava (defaults) check..."
./gradlew metalavaCheckCompatibilityDefaultsRelease -q
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  echo "❌ metalava for defaults flavor failed, running it for you..."

  ./gradlew metalavaGenerateSignatureDefaultsRelease -q

  echo "API dump done. Please check the updated API dump and add it to your commit."
  exit $EXIT_CODE
fi

echo "Running metalava (custom entitlement) check..."
./gradlew metalavaCheckCompatibilityCustomEntitlementComputationRelease -q
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  echo "❌ metalava for custom entitlement flavor failed, running it for you..."

  ./gradlew metalavaGenerateSignatureCustomEntitlementComputationRelease -q

  echo "API dump done. Please check the updated API dump and add it to your commit."
  exit $EXIT_CODE
fi
