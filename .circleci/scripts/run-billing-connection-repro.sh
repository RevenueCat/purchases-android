#!/usr/bin/env bash
set -euo pipefail

serial="${ANDROID_SERIAL:-emulator-5554}"
artifacts="${BILLING_REPRO_ARTIFACTS:-/tmp/billing-repro}"
mkdir -p "$artifacts"

adb -s "$serial" shell pm path com.android.vending | grep -q '^package:' || {
    echo 'The emulator does not have the Play Store installed.' >&2
    exit 1
}
adb -s "$serial" install -r purchases/build/outputs/apk/androidTest/defaults/debug/purchases-defaults-debug-androidTest.apk

# This emulator is dedicated to the repro. Stop the churn even if the test fails.
(
    while true; do
        adb -s "$serial" shell am force-stop com.android.vending >/dev/null 2>&1
        sleep 0.15
    done
) &
churn_pid=$!
# shellcheck disable=SC2329 # Invoked by the EXIT trap.
cleanup() {
    kill "$churn_pid" 2>/dev/null || true
    wait "$churn_pid" 2>/dev/null || true
}
trap cleanup EXIT

for attempt in {1..10}; do
    echo "Billing connection repro: attempt $attempt of 10"
    adb -s "$serial" logcat -c
    result="$artifacts/attempt-$attempt.txt"
    if ! adb -s "$serial" shell am instrument -w \
        -e billingRepro true \
        -e class com.revenuecat.purchases.google.ConnectionReproTest \
        com.revenuecat.purchases.integrationTest/androidx.test.runner.AndroidJUnitRunner > "$result" 2>&1; then
        cat "$result"
        echo 'Instrumentation failed to run.' >&2
        exit 1
    fi
    adb -s "$serial" logcat -d -v threadtime > "$artifacts/attempt-$attempt-logcat.txt"
    cat "$result"
    if grep -q 'OK (1 test)' "$result"; then
        echo 'Billing service death and both timeout callbacks observed.'
        exit 0
    fi
    if grep -q 'race may not have reproduced\|Billing service death was not observed' "$result"; then
        echo 'Race not observed; retrying.'
    else
        echo 'Test failed after triggering the repro or failed unexpectedly.' >&2
        exit 1
    fi
done

echo 'Race was not observed after 10 attempts; repro is inconclusive.' >&2
exit 1
