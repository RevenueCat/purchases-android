#!/usr/bin/env bash
set -eo pipefail

# Run lint only on the first container
if [ "$CIRCLE_NODE_INDEX" = "0" ]; then
  ./gradlew lint
fi

# Gather test files and split them across parallel nodes
TEST_FILES=$(circleci tests glob "**/src/test/**/?(*Test).kt" "**/src/test/**/?(*Test).java" | circleci tests split)

# Convert file paths to Gradle test class names
TEST_ARGS=""
for file in $TEST_FILES; do
  class_name=$(echo "$file" | sed -e 's#^./##' -e 's#.*/src/test/java/##' -e 's#.*/src/test/kotlin/##' -e 's#.kt$##' -e 's#.java$##' -e 's#/#.#g')
  TEST_ARGS+=" --tests $class_name"
done

if [ -z "$TEST_ARGS" ]; then
  echo "No tests to run on this node"
  exit 0
fi

./gradlew test $TEST_ARGS
