#!/usr/bin/env python3
"""Post-processes flatc's Kotlin output for the FlatBuffers PoC.

flatc emits one class per file containing BOTH read accessors and write (FlatBufferBuilder)
helpers, with a `public` visibility and imports against `com.google.flatbuffers`. This script
turns one flatc file into one of two flavors:

  main  -> reader-only, shipped by the SDK. Drops every `com.google.flatbuffers` import (the base
           class `Table`/`Constants` are the vendored package-private runtime in the SAME package)
           and the unused `kotlin.math.sign` import, marks the class `internal`, and deletes every
           companion function that takes a `FlatBufferBuilder` (the writer surface). The SDK thus
           needs no FlatBufferBuilder and no external flatbuffers dependency.

  test  -> full read+write accessors, test-only. Repackages `...flatbuffers.generated` to
           `...flatbuffers.testgen` and marks the class `internal`, keeping the real
           `com.google.flatbuffers` imports (resolved via the test-only dependency). Used by the
           encoder fixture (the "backend side").

Usage: flatc_postprocess.py <main|test>   (reads stdin, writes stdout)
"""
import re
import sys

MAIN_NOTE = [
    "//",
    "// Reader-only: the writer companion functions (those taking a FlatBufferBuilder) are stripped",
    "// by scripts/generate-flatbuffers.sh so the shipped SDK needs no FlatBufferBuilder. The base",
    "// class Table and Constants are the vendored, package-private runtime under src/main/java",
    "// (same package). See purchases/src/flatbuffers/README.md.",
]

TEST_NOTE = [
    "//",
    "// Test-only FULL accessors (read + write). The backend/encoder side of the PoC builds buffers",
    "// with these against the real com.google.flatbuffers runtime (testImplementation). The shipped",
    "// SDK instead uses the reader-only copy under src/main with a vendored runtime. Emitted into",
    "// the `testgen` package by scripts/generate-flatbuffers.sh. See purchases/src/flatbuffers/README.md.",
]


def transform_main(lines):
    out = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        stripped = line.strip()

        # Drop the @ExperimentalUnsignedTypes annotation when it precedes a builder function.
        if stripped.startswith("@kotlin.ExperimentalUnsignedTypes"):
            nxt = lines[i + 1].strip() if i + 1 < n else ""
            if nxt.startswith("fun ") and "FlatBufferBuilder" in nxt:
                i += 1
                continue
            out.append(line)
            i += 1
            continue

        # Delete companion writer functions (those taking a FlatBufferBuilder).
        if stripped.startswith("fun ") and "FlatBufferBuilder" in line:
            if line.rstrip().endswith("{"):
                depth = line.count("{") - line.count("}")
                i += 1
                while i < n and depth > 0:
                    depth += lines[i].count("{") - lines[i].count("}")
                    i += 1
                continue
            i += 1
            continue

        # Drop imports the reader does not need.
        if stripped.startswith("import com.google.flatbuffers") or stripped == "import kotlin.math.sign":
            i += 1
            continue

        if re.match(r"^class \w", line):
            line = "internal " + line

        out.append(line)
        i += 1
    return out


def transform_test(lines):
    out = []
    for line in lines:
        line = line.replace(
            "com.revenuecat.purchases.flatbuffers.generated",
            "com.revenuecat.purchases.flatbuffers.testgen",
        )
        if re.match(r"^class \w", line):
            line = "internal " + line
        out.append(line)
    return out


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else ""
    lines = sys.stdin.read().split("\n")
    if mode == "main":
        lines = transform_main(lines)
        note = MAIN_NOTE
    elif mode == "test":
        lines = transform_test(lines)
        note = TEST_NOTE
    else:
        sys.stderr.write("usage: flatbuffers_postprocess.py <main|test>\n")
        sys.exit(2)

    # Insert the explanatory note right after flatc's first "// automatically generated" line.
    if lines and lines[0].startswith("// automatically generated"):
        lines = [lines[0]] + note + lines[1:]

    text = "\n".join(lines).rstrip("\n") + "\n"
    sys.stdout.write(text)


if __name__ == "__main__":
    main()
