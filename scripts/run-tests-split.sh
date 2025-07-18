#!/usr/bin/env bash
set -eo pipefail

# Run lint only on the first container
if [ "$CIRCLE_NODE_INDEX" = "0" ]; then
  ./gradlew lint
fi

# Gather test files and split them across parallel nodes. Include variations like
# src/testDefaults and src/testCustomEntitlementComputation. Focus on unit tests only
# since './gradlew test' doesn't run Android instrumentation tests.
# If the CircleCI CLI is unavailable (e.g. when running locally), fall back to finding all tests
# without splitting.
PATTERNS=("**/src/test*/**/*Test.kt" "**/src/test*/**/*Test.java")
if command -v circleci >/dev/null 2>&1; then
  TEST_FILES=$(circleci tests glob "${PATTERNS[@]}" | circleci tests split)
else
  echo "circleci CLI not found; running all tests without splitting" >&2
  TEST_FILES=$(find . -path "*src/test*" \( -name "*Test.kt" -o -name "*Test.java" \))
fi

# List of known JVM modules that use the generic 'test' task (not Android variant tasks)
JVM_MODULES=(":bom" ":dokka-hide-internal")

# Function to check if a module is a JVM module
is_jvm_module() {
  local module="$1"
  for jvm_module in "${JVM_MODULES[@]}"; do
    if [ "$module" = "$jvm_module" ]; then
      return 0
    fi
  done
  return 1
}

# Group test files by module and variant to use appropriate test tasks
declare -A module_variant_tests

for file in $TEST_FILES; do
  # Extract module name, handling nested modules like ui/revenuecatui
  # Look for the pattern before /src/ to get the full module path
  module=$(echo "$file" | sed -E 's|^\.?/?([^/]+.*)/src/.*|\1|')
  
  # Convert filesystem path to Gradle project path (replace / with :)
  # ui/revenuecatui -> :ui:revenuecatui
  module_gradle_path=":$(echo "$module" | sed 's|/|:|g')"
  
  # Extract variant from test path (e.g., "testDefaults" -> "Defaults")
  variant=""
  if echo "$file" | grep -q "/src/test[^/]*/"; then
    # Extract the test directory name (e.g., "testDefaults", "test")
    variant_path=$(echo "$file" | sed -E 's|.*/src/(test[^/]*)/.*|\1|')
    if [[ "$variant_path" != "test" ]]; then
      # Remove "test" prefix to get just the variant
      variant=$(echo "$variant_path" | sed 's/^test//')
      # Capitalize first letter if variant is not empty
      if [ -n "$variant" ]; then
        variant="$(echo ${variant:0:1} | tr '[:lower:]' '[:upper:]')${variant:1}"
      fi
    fi
  fi
  
  # Convert file path to test class name
  class_name=$(echo "$file" | sed \
    -e 's|^\./||' \
    -e 's|.*/src/test[^/]*/java/||' \
    -e 's|.*/src/test[^/]*/kotlin/||' \
    -e 's|\.kt$||' \
    -e 's|\.java$||' \
    -e 's|/|.|g')
  
  # Create key for grouping: module_gradle_path|variant (using | as separator)
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
  
  if [ -z "$variant" ]; then
    # For tests in src/test (no variant), check if it's a JVM module
    if is_jvm_module "$module_path"; then
      echo "Running JVM tests for $module_path"
      ./gradlew "${module_path}:test" $(echo "$classes" | sed 's/\([^ ]*\)/--tests \1/g')
    else
      echo "Running Android unit tests for $module_path (both Debug and Release)"
      # Run both Debug and Release variants to match './gradlew test' behavior
      ./gradlew "${module_path}:testDefaultsDebugUnitTest" $(echo "$classes" | sed 's/\([^ ]*\)/--tests \1/g')
      ./gradlew "${module_path}:testDefaultsReleaseUnitTest" $(echo "$classes" | sed 's/\([^ ]*\)/--tests \1/g')
    fi
  else
    # For variant-specific tests, run both Debug and Release
    echo "Running Android unit tests for $module_path with variant $variant (both Debug and Release)"
    debug_task_name="${module_path}:test${variant}DebugUnitTest"
    release_task_name="${module_path}:test${variant}ReleaseUnitTest"
    ./gradlew "$debug_task_name" $(echo "$classes" | sed 's/\([^ ]*\)/--tests \1/g')
    ./gradlew "$release_task_name" $(echo "$classes" | sed 's/\([^ ]*\)/--tests \1/g')
  fi
done
