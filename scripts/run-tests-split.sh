#!/usr/bin/env bash
set -eo pipefail

# Support dry-run mode for verification
DRY_RUN=${DRY_RUN:-false}

# Create debug log file for CircleCI artifacts
mkdir -p build/test-split-debug
DEBUG_LOG="build/test-split-debug/test-split-analysis-node-${CIRCLE_NODE_INDEX:-local}.log"

# Function to log to both console and file
debug_log() {
  echo "$1" | tee -a "$DEBUG_LOG"
}

# Run lint only on the first container (skip in dry-run mode)
if [ "$CIRCLE_NODE_INDEX" = "0" ] && [ "$DRY_RUN" != "true" ]; then
  ./gradlew lint
elif [ "$CIRCLE_NODE_INDEX" = "0" ] && [ "$DRY_RUN" = "true" ]; then
  debug_log "🔍 DRY RUN: Skipping lint task"
fi

debug_log "=== DEBUG: Test Split Script Analysis ==="
debug_log "Node: ${CIRCLE_NODE_INDEX:-local} of ${CIRCLE_NODE_TOTAL:-1}"
if [ "$DRY_RUN" = "true" ]; then
  debug_log "🔍 DRY RUN MODE: Tasks will be logged but not executed"
fi

# Step 1: Discover all test tasks that would run with './gradlew test'
debug_log ""
debug_log "=== Discovering Test Tasks ==="

# Get all test tasks that './gradlew test' would execute
# Focus on actual test execution tasks, not compilation tasks  
ALL_TEST_TASKS=$(./gradlew test --dry-run 2>/dev/null | grep 'SKIPPED' | sed 's/ SKIPPED$//' | grep -E ':.*:test[^:]*$' | sort)

# Use all tasks that './gradlew test' would run - trust Gradle's decision
VALID_TEST_TASKS="$ALL_TEST_TASKS"
TOTAL_TASKS=$(echo "$VALID_TEST_TASKS" | wc -l | tr -d ' ')

debug_log ""
debug_log "Total test tasks found: $TOTAL_TASKS"
debug_log "Test tasks to split:"
echo "$VALID_TEST_TASKS" | tee -a "$DEBUG_LOG"

# Step 2: Split tasks across CircleCI nodes
debug_log ""
debug_log "=== Splitting Tasks Across Nodes ==="

if [ -z "$VALID_TEST_TASKS" ]; then
  debug_log "No valid test tasks found"
  exit 0
fi

# Use CircleCI's built-in test splitting on the task list
if command -v circleci >/dev/null 2>&1 && circleci tests glob --help >/dev/null 2>&1; then
  # Write tasks to a temp file for CircleCI to split
  TASKS_FILE=$(mktemp)
  echo "$VALID_TEST_TASKS" > "$TASKS_FILE"
  
  # Split tasks using CircleCI
  MY_TASKS=$(circleci tests split "$TASKS_FILE" --split-by=timings --timings-type=classname)
  rm "$TASKS_FILE"
  
  debug_log "Tasks assigned to this node by CircleCI:"
  echo "$MY_TASKS" | tee -a "$DEBUG_LOG"
else
  # Fallback: simple round-robin splitting for local testing
  debug_log "CircleCI CLI not found; using round-robin splitting for local testing"
  
  NODE_INDEX=${CIRCLE_NODE_INDEX:-0}
  NODE_TOTAL=${CIRCLE_NODE_TOTAL:-1}
  
  MY_TASKS=$(echo "$VALID_TEST_TASKS" | awk -v node="$NODE_INDEX" -v total="$NODE_TOTAL" 'NR % total == node')
  
  debug_log "Tasks assigned to node $NODE_INDEX of $NODE_TOTAL:"
  echo "$MY_TASKS" | tee -a "$DEBUG_LOG"
fi

ASSIGNED_TASK_COUNT=$(echo "$MY_TASKS" | grep -v '^$' | wc -l | tr -d ' ')

if [ "$ASSIGNED_TASK_COUNT" -eq 0 ]; then
  debug_log "No tasks assigned to this node"
  exit 0
fi

# Step 3: Execute assigned tasks (or dry-run)
debug_log ""
if [ "$DRY_RUN" = "true" ]; then
  debug_log "=== DRY RUN: Tasks That Would Be Executed ==="
  debug_log "Would run $ASSIGNED_TASK_COUNT tasks on this node"
else
  debug_log "=== Executing Assigned Tasks ==="
  debug_log "Running $ASSIGNED_TASK_COUNT tasks on this node"
fi

# Track all tasks for summary
declare -a executed_tasks=()

for task in $MY_TASKS; do
  if [ -n "$task" ]; then
    if [ "$DRY_RUN" = "true" ]; then
      debug_log "Would execute: $task"
    else
      debug_log "Executing: $task"
      ./gradlew "$task"
    fi
    executed_tasks+=("$task")
  fi
done

debug_log ""
debug_log "=== SUMMARY ==="
debug_log "Total tasks in project: $TOTAL_TASKS"
debug_log "Tasks assigned to this node: $ASSIGNED_TASK_COUNT"
if [ "$DRY_RUN" = "true" ]; then
  debug_log "Tasks that would be executed:"
else
  debug_log "Tasks executed successfully:"
fi
for task in "${executed_tasks[@]}"; do
  debug_log "  - $task"
done
debug_log ""
debug_log "=== END SUMMARY ==="

echo "Debug log saved to: $DEBUG_LOG" 