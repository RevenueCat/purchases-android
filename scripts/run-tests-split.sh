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

# Group test files by module and variant to use appropriate test tasks
declare -A module_variant_tests

for file in $TEST_FILES; do
  # Extract module name, handling nested modules like ui/revenuecatui
  # Look for the pattern before /src/ to get the full module path
  module=$(echo "$file" | sed -E 's|^\.?/?([^/]+.*)/src/.*|\1|')
  
  # Convert filesystem path to Gradle project path (replace / with :)
  # ui/revenuecatui -> :ui:revenuecatui
  module_gradle_path=":$(echo "$module" | sed 's|/|:|g')"
  
  # Extract variant from test path (e.g., "testDefaults" -> "Defaults", "test" -> "")
  variant=""
  if echo "$file" | grep -q "/src/test[^/]*/"; then
    variant_path=$(echo "$file" | sed -E 's|.*/src/(test[^/]*)/.*|\1|')
    if [ "$variant_path" != "test" ]; then
      # Convert testDefaults -> Defaults, testCustomEntitlementComputation -> CustomEntitlementComputation
      variant=$(echo "$variant_path" | sed 's/^test//')
      # Capitalize first letter
      variant="$(echo ${variant:0:1} | tr '[:lower:]' '[:upper:]')${variant:1}"
    fi
  fi
  
  # For tests in src/test (not variant-specific), default to "Defaults" for Android modules
  # This handles the case where Android modules have tests in src/test but still need variant tasks
  if [ -z "$variant" ]; then
    variant="Defaults"
  fi
  
  # Convert file path to test class name
  class_name=$(echo "$file" | sed \
    -e 's|^\./||' \
    -e 's|.*/src/test[^/]*/java/||' \
    -e 's|.*/src/test[^/]*/kotlin/||' \
    -e 's|\.kt$||' \
    -e 's|\.java$||' \
    -e 's|/|.|g')
  
  # Create key for grouping: module_gradle_path|variant (using | as separator to avoid confusion with colons)
  key="${module_gradle_path}|${variant}"
  
  # Add class to the group
  if [ -z "${module_variant_tests[$key]}" ]; then
    module_variant_tests[$key]="$class_name"
  else
    module_variant_tests[$key]="${module_variant_tests[$key]} $class_name"
  fi
done

if [ ${#module_variant_tests[@]} -eq 0 ]; then
  echo "No tests to run on this node"
  exit 0
fi

# Run tests for each module:variant combination
for key in "${!module_variant_tests[@]}"; do
  module_path=$(echo "$key" | cut -d'|' -f1)
  variant=$(echo "$key" | cut -d'|' -f2)
  classes="${module_variant_tests[$key]}"
  
  # Always try the Android variant test task first
  echo "Running Android tests for $module_path with variant $variant"
  task_name="${module_path}:test${variant}DebugUnitTest"
  ./gradlew "$task_name" $(echo "$classes" | sed 's/\([^ ]*\)/--tests \1/g')
done
