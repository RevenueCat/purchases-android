#!/usr/bin/env bash

exit_code=0

# Metalava signature files; only these should gate the check so unrelated
# tracked-file churn (e.g. mise.lock) doesn't fail it.
api_paths=(':(glob)**/api.txt' ':(glob)**/api-*.txt')

# Run the api-dump.sh script to generate the API dump
./scripts/api-dump.sh || exit_code=$?
# Check if there are any dirty changes in the API signature files
if ! git diff --quiet -- "${api_paths[@]}"; then
  echo "Diff:"
  git --no-pager diff -- "${api_paths[@]}"
  echo
  echo "API dump has changes, run the scripts/api-dump.sh script to generate the API dump, review and commit them." >&2
  exit_code=1
fi

exit $exit_code
