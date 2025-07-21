#!/usr/bin/env bash
set -eo pipefail

# Create debug log file for CircleCI artifacts
mkdir -p build/test-split-debug
DEBUG_LOG="build/test-split-debug/test-split-analysis-node-${CIRCLE_NODE_INDEX:-local}.log"

# Function to log to both console and file
debug_log() {
  echo "$1" | tee -a "$DEBUG_LOG"
}

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
if command -v circleci >/dev/null 2>&1 && circleci tests glob --help >/dev/null 2>&1; then
  TEST_FILES=$(circleci tests glob "${PATTERNS[@]}" | circleci tests split)
else
  echo "circleci CLI not found or not in job context; running all tests without splitting" >&2
  TEST_FILES=$(find . -path "*src/test*" \( -name "*Test.kt" -o -name "*Test.java" \))
fi

# Debug logging
debug_log "=== DEBUG: Test Split Script Analysis ==="
debug_log "Node: ${CIRCLE_NODE_INDEX:-local} of ${CIRCLE_NODE_TOTAL:-1}"
debug_log "Test files found: $(echo "$TEST_FILES" | wc -l)"
debug_log "Test files:"
echo "$TEST_FILES" | tee -a "$DEBUG_LOG"
debug_log ""

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
declare -a all_tasks_to_run

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
  
  # Debug: Show file processing
  debug_log "File: $file -> Module: $module_gradle_path, Variant: '$variant', Class: $class_name"
done

debug_log ""
debug_log "=== Grouped Tests by Module:Variant ==="
for key in "${!module_variant_tests[@]}"; do
  module_path=$(echo "$key" | cut -d'|' -f1)
  variant=$(echo "$key" | cut -d'|' -f2)
  classes="${module_variant_tests[$key]}"
  class_count=$(echo "$classes" | wc -w)
  debug_log "$key -> $class_count classes: $classes"
done
debug_log ""

if [ ${#module_variant_tests[@]} -eq 0 ]; then
  debug_log "No tests to run on this node"
  exit 0
fi

# Run tests for each module:variant combination
debug_log "=== Tasks to Execute ==="
for key in "${!module_variant_tests[@]}"; do
  module_path=$(echo "$key" | cut -d'|' -f1)
  variant=$(echo "$key" | cut -d'|' -f2)
  classes="${module_variant_tests[$key]}"
  test_args=$(echo "$classes" | sed 's/\([^ ]*\)/--tests \1/g')
  
  if [ -z "$variant" ]; then
    # For tests in src/test (no variant), check if it's a JVM module
    if is_jvm_module "$module_path"; then
      task="${module_path}:test"
      debug_log "JVM: $task $test_args"
      all_tasks_to_run+=("$task")
      ./gradlew "$task" $test_args
    else
      debug_task="${module_path}:testDefaultsDebugUnitTest"
      release_task="${module_path}:testDefaultsReleaseUnitTest"
      debug_log "Android (defaults): $debug_task $test_args"
      debug_log "Android (defaults): $release_task $test_args"
      all_tasks_to_run+=("$debug_task" "$release_task")
      # Run both Debug and Release variants to match './gradlew test' behavior
      ./gradlew "$debug_task" $test_args
      ./gradlew "$release_task" $test_args
    fi
  else
    # For variant-specific tests, run both Debug and Release
    debug_task_name="${module_path}:test${variant}DebugUnitTest"
    release_task_name="${module_path}:test${variant}ReleaseUnitTest"
    debug_log "Android ($variant): $debug_task_name $test_args"
    debug_log "Android ($variant): $release_task_name $test_args"
    all_tasks_to_run+=("$debug_task_name" "$release_task_name")
    ./gradlew "$debug_task_name" $test_args
    ./gradlew "$release_task_name" $test_args
  fi
done

debug_log ""
debug_log "=== SUMMARY ==="
debug_log "Total test files processed: $(echo "$TEST_FILES" | wc -l)"
debug_log "Total task executions: ${#all_tasks_to_run[@]}"
debug_log "Tasks executed:"
for task in "${all_tasks_to_run[@]}"; do
  debug_log "  - $task"
done
debug_log ""
debug_log "To compare with full test run, execute:"
debug_log "  ./gradlew test --dry-run | grep ':test' | grep 'SKIPPED' | sort"
debug_log "=== END SUMMARY ==="

echo "Debug log saved to: $DEBUG_LOG"
