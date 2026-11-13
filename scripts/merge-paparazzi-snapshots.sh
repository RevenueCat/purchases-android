#!/usr/bin/env bash
set -euo pipefail

# Merges the Paparazzi snapshots staged by the record-paparazzi-* CI jobs back into each module's
# snapshot directory so they can be uploaded to Emerge as one set.
#
# The record jobs each run an explicit --tests list, so a snapshot test class that none of them names
# silently drops its snapshots from Emerge. Paparazzi names every image <package>_<class>_<method>.png,
# so the merge fails first if no staged image matches a snapshot test class.

staged_dir="paparazzi-snapshots"
revenuecatui_images="ui/revenuecatui/src/test/snapshots/images"
debugview_images="ui/debugview/src/test/snapshots/images"
snapshot_tests_dirs=(ui/revenuecatui/src/test*/kotlin/com/revenuecat/purchases/ui/revenuecatui/snapshottests)

missing=""
for file in $(grep -rl '@Test' "${snapshot_tests_dirs[@]}"); do
  class=$(basename "$file" .kt)
  if ! ls "$staged_dir"/*/revenuecatui/*"_${class}_"*.png >/dev/null 2>&1; then
    missing="$missing $class"
  fi
done
if [ -n "$missing" ]; then
  echo "No snapshots were recorded for:$missing" >&2
  echo "Add each class to the revenuecatui_tests patterns of a record-paparazzi-* job in .circleci/config.yml" >&2
  exit 1
fi

rm -rf "$revenuecatui_images" "$debugview_images"
mkdir -p "$revenuecatui_images" "$debugview_images"
for dir in "$staged_dir"/*/revenuecatui; do cp -r "$dir"/. "$revenuecatui_images"/; done
for dir in "$staged_dir"/*/debugview; do cp -r "$dir"/. "$debugview_images"/; done
echo "revenuecatui: $(find "$revenuecatui_images" -name '*.png' | wc -l) snapshots"
echo "debugview: $(find "$debugview_images" -name '*.png' | wc -l) snapshots"
