#!/usr/bin/env bash

echo "Running detekt check..."
OUTPUT="/tmp/detekt-$(date +%s)"
./gradlew detektAll > $OUTPUT
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  cat $OUTPUT
  rm $OUTPUT
  echo "***********************************************"
  echo "                 Detekt failed                 "
  echo " Please fix the above issues before committing "
  echo "***********************************************"
  exit $EXIT_CODE
fi
rm $OUTPUT

./gradlew metalavaCheckCompatibilityDefaultsRelease -q
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  echo "❌ metalava for defaults flavor failed, running it for you..."

  ./gradlew metalavaCheckCompatibilityDefaultsRelease -q

  echo "API dump done, please check the results and then try your commit again!"
  exit $EXIT_CODE
fi


./gradlew metalavaCheckCompatibilityCustomEntitlementComputationRelease -q
EXIT_CODE=$?
if [ $EXIT_CODE -ne 0 ]; then
  echo "❌ metalava for custom entitlement flavor failed, running it for you..."

  ./gradlew metalavaCheckCompatibilityCustomEntitlementComputationRelease -q

  echo "API dump done, please check the results and then try your commit again!"
  exit $EXIT_CODE
fi

exit 0