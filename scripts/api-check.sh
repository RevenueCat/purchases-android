#!/usr/bin/env bash

exit_code=0

# Run the api-dump.sh script to generate the API dump
./scripts/api-dump.sh || exit_code=$?
# Check if there are any dirty changes in git
if ! git diff --quiet; then
  echo "API dump has changes, please review and commit them."
  exit_code=1
fi

exit $exit_code
