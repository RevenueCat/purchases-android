#!/usr/bin/env bash
set -eo pipefail

# Run lint only on the first container
if [ "$CIRCLE_NODE_INDEX" = "0" ]; then
  ./gradlew lint
fi

# Gather test files and split them across parallel nodes. Include variations like
# src/testDefaults and src/testCustomEntitlementComputation. If the CircleCI CLI
# is unavailable (e.g. when running locally), fall back to finding all tests
# without splitting.
PATTERNS=("**/src/test*/**/*Test.kt" "**/src/test*/**/*Test.java")
if command -v circleci >/dev/null 2>&1; then
  TEST_FILES=$(circleci tests glob "${PATTERNS[@]}" | circleci tests split)
else
  echo "circleci CLI not found; running all tests without splitting" >&2
  TEST_FILES=$(find . -path "*src/test*" \( -name "*Test.kt" -o -name "*Test.java" \))
fi

# Convert file paths to Gradle test class names
TEST_ARGS=""
for file in $TEST_FILES; do
  class_name=$(echo "$file" | sed \
    -e 's#^./##' \
    -e 's#.*/src/test[^/]*/java/##' \
    -e 's#.*/src/test[^/]*/kotlin/##' \
    -e 's#.kt$##' \
    -e 's#.java$##' \
    -e 's#/#.#g')
  TEST_ARGS+=" --tests $class_name"
done

if [ -z "$TEST_ARGS" ]; then
  echo "No tests to run on this node"
  exit 0
fi

./gradlew test $TEST_ARGS
