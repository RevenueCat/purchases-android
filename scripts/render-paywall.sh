#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

usage() {
    echo "Usage: $0 --input <offerings.json> --output <screenshot.png> [--offering-id <id>]"
    echo ""
    echo "Renders an Android paywall screenshot from an SDK-shaped offerings.json."
    echo ""
    echo "Arguments:"
    echo "  --input        Path to an SDK-shaped offerings.json file"
    echo "  --output       Path where the output PNG will be written"
    echo "  --offering-id  Optional offering identifier to render; defaults to offerings.current"
    exit 1
}

INPUT_FILE=""
OUTPUT_FILE=""
OFFERING_ID=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --input)        INPUT_FILE="$2"; shift 2 ;;
        --output)       OUTPUT_FILE="$2"; shift 2 ;;
        --offering-id)  OFFERING_ID="$2"; shift 2 ;;
        *)              echo "Unknown argument: $1"; usage ;;
    esac
done

[[ -z "$INPUT_FILE" ]] && { echo "Error: --input is required"; usage; }
[[ -z "$OUTPUT_FILE" ]] && { echo "Error: --output is required"; usage; }
[[ ! -f "$INPUT_FILE" ]] && { echo "Error: Input file not found: $INPUT_FILE"; exit 1; }
command -v jq >/dev/null 2>&1 || { echo "Error: jq is required but not installed"; exit 1; }

INPUT_FILE="$(cd "$(dirname "$INPUT_FILE")" && pwd)/$(basename "$INPUT_FILE")"
OUTPUT_FILE="$(cd "$(dirname "$OUTPUT_FILE")" && pwd)/$(basename "$OUTPUT_FILE")"

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

cp "$INPUT_FILE" "$WORK_DIR/offerings.json"

echo "==> Extracting image URLs from offerings.json..."

extract_icon_urls() {
    jq -r '
        recurse | select(type=="object" and .type=="icon")
        | . as $icon
        | "\($icon.base_url)/\($icon.formats.webp)",
          "\($icon.base_url)/\($icon.formats.heic)",
          (.overrides[]? | select(.properties.formats.webp) | "\($icon.base_url)/\(.properties.formats.webp)"),
          (.overrides[]? | select(.properties.formats.heic) | "\($icon.base_url)/\(.properties.formats.heic)")
    ' "$1" 2>/dev/null || true
}

extract_image_urls() {
    jq -r '
        recurse | select(type=="object" and .type=="image")
        | . as $img
        | (
            "\($img.source.light.webp)",
            "\($img.source.dark.webp)",
            "\($img.source.light.heic)",
            "\($img.source.dark.heic)",
            (.overrides[]? | select(.properties.source) |
                "\(.properties.source.light.webp)",
                "\(.properties.source.dark.webp)",
                "\(.properties.source.light.heic)",
                "\(.properties.source.dark.heic)"
            )
          )
        | select(. != "null")
    ' "$1" 2>/dev/null || true
}

extract_background_urls() {
    jq -r '
        recurse | select(.background? and .background.type=="image")
        | .background.value
        | (.light.webp, .dark.webp, .light.heic, .dark.heic)
        | select(. != null)
    ' "$1" 2>/dev/null || true
}

ALL_URLS=$(
    {
        extract_icon_urls "$WORK_DIR/offerings.json"
        extract_image_urls "$WORK_DIR/offerings.json"
        extract_background_urls "$WORK_DIR/offerings.json"
    } | sort -u | grep -v '^$' || true
)

URL_COUNT=$(echo "$ALL_URLS" | grep -c . || true)
echo "    Found $URL_COUNT unique URLs to download."

download_asset() {
    local url="$1"
    local target_dir="$2"

    local host path
    host="$(echo "$url" | sed -E 's|https?://([^/]+).*|\1|')"
    path="$(echo "$url" | sed -E 's|https?://[^/]+(/.*)|\1|')"

    # Reverse host parts, dropping TLD: assets.pawwalls.com -> pawwalls/assets
    local reversed
    reversed="$(echo "$host" | awk -F. '{for(i=NF-1;i>=1;i--) printf "%s%s",$i,(i>1?"/":"")}')"

    local file_path="${target_dir}/${reversed}${path}"
    mkdir -p "$(dirname "$file_path")"
    curl -sS -L -o "$file_path" "$url"
}

if [[ -n "$ALL_URLS" ]]; then
    echo "==> Downloading paywall assets..."
    while IFS= read -r url; do
        [[ -z "$url" ]] && continue
        echo "    $url"
        download_asset "$url" "$WORK_DIR"
    done <<< "$ALL_URLS"
    echo "    Done."
fi

PAPARAZZI_OUTPUT_DIR="$REPO_ROOT/ui/revenuecatui/src/test/snapshots/images"

echo "==> Running Paparazzi screenshot test..."
cd "$REPO_ROOT"
./gradlew :ui:revenuecatui:recordPaparazziBc8Debug \
    --tests="*PaywallJsonScreenshotter" \
    -Ppaywall.input.dir="$WORK_DIR" \
    -Ppaywall.offering.id="$OFFERING_ID"

PNG_FILE=$(find "$PAPARAZZI_OUTPUT_DIR" -name '*PaywallJsonScreenshotter*' -name '*.png' | head -1)
if [[ -z "$PNG_FILE" ]]; then
    echo "Error: No screenshot PNG found in $PAPARAZZI_OUTPUT_DIR"
    exit 1
fi

cp "$PNG_FILE" "$OUTPUT_FILE"
echo "==> Screenshot saved to: $OUTPUT_FILE"
