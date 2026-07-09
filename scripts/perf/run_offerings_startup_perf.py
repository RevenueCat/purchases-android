#!/usr/bin/env python3
"""Driver for the offerings startup performance harness.

Runs the OfferingsStartupPerfTest instrumentation test across a matrix of scenarios and cache modes,
pulls the per-run JSON result files off the device, and aggregates them into a summary table:

    scenario | cache | network | project | n | p50 | p90 | p95 | max | errors | fallbacks

Network shaping is applied OUTSIDE this script (emulator console, tc netem, ...). Pass --network-label so
results are tagged with the conditions they ran under. See scripts/perf/README.md.

Usage examples:

    # Full default matrix, 10 iterations per cell, against a perf project:
    python3 scripts/perf/run_offerings_startup_perf.py run \
        --api-key $PERF_TEST_API_KEY --network-label ideal --project-label large-copy --iterations 10

    # A single cell:
    python3 scripts/perf/run_offerings_startup_perf.py run \
        --api-key $PERF_TEST_API_KEY --scenarios workflows --cache-modes warm_offerings_cold_config

    # Re-aggregate previously pulled results:
    python3 scripts/perf/run_offerings_startup_perf.py aggregate --results-dir perf-results
"""

import argparse
import json
import math
import shlex
import subprocess
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
TEST_PACKAGE = "com.revenuecat.purchases.integrationTest"
TEST_RUNNER = f"{TEST_PACKAGE}/androidx.test.runner.AndroidJUnitRunner"
TEST_CLASS = "com.revenuecat.purchases.perftests.OfferingsStartupPerfTest"
TEST_APK = (
    REPO_ROOT
    / "purchases/build/outputs/apk/androidTest/defaults/debug/purchases-defaults-debug-androidTest.apk"
)
DEVICE_RESULTS_DIR = f"/sdcard/Android/data/{TEST_PACKAGE}/files/revenuecat-perf"

ALL_SCENARIOS = [
    "baseline",
    "workflows",
    "workflows_config_404",
    "workflows_config_500",
    "workflows_config_unreachable",
]
ALL_CACHE_MODES = ["cold", "warm_disk", "warm_memory", "warm_offerings_cold_config"]
ALL_LIFECYCLE_MODES = ["foreground", "cold_start_ordering", "foreground_return"]


def is_valid_cell(scenario, cache, lifecycle):
    # warm_offerings_cold_config is a no-op distinction for baseline (there is no config cache to be cold).
    if scenario == "baseline" and cache == "warm_offerings_cold_config":
        return False
    # cold_start_ordering configures the SDK in the timed phase; warm_memory reuses a live instance.
    if lifecycle == "cold_start_ordering" and cache == "warm_memory":
        return False
    # foreground_return measures a live, already-configured instance returning to the foreground.
    if lifecycle == "foreground_return" and cache != "warm_memory":
        return False
    return True


DEFAULT_MATRIX = [
    (scenario, cache, lifecycle)
    for scenario in ALL_SCENARIOS
    for cache in ALL_CACHE_MODES
    for lifecycle in ALL_LIFECYCLE_MODES
    if is_valid_cell(scenario, cache, lifecycle)
]


def run(cmd, check=True, capture=False, timeout=None):
    print(f"$ {' '.join(shlex.quote(str(c)) for c in cmd)}", flush=True)
    return subprocess.run(
        [str(c) for c in cmd],
        check=check,
        capture_output=capture,
        text=True,
        timeout=timeout,
    )


def adb(args, serial=None, **kwargs):
    cmd = ["adb"]
    if serial:
        cmd += ["-s", serial]
    return run(cmd + args, **kwargs)


def build_apk():
    gradlew = REPO_ROOT / "gradlew"
    run([gradlew, ":purchases:assembleDefaultsDebugAndroidTest"], check=True)
    if not TEST_APK.exists():
        sys.exit(f"Expected test APK not found at {TEST_APK}")


def install_apk(serial):
    adb(["install", "-r", "-t", TEST_APK], serial=serial)


def run_cell(serial, scenario, cache_mode, lifecycle_mode, args):
    """Runs one scenario x cache-mode x lifecycle-mode cell. Returns True on instrumentation success."""
    instrument_args = {
        "class": TEST_CLASS,
        "PERF_TEST_API_KEY": args.api_key,
        "PERF_TEST_SCENARIO": scenario,
        "PERF_TEST_CACHE_MODE": cache_mode,
        "PERF_TEST_LIFECYCLE_MODE": lifecycle_mode,
        "PERF_TEST_ITERATIONS": str(args.iterations),
        "PERF_TEST_NETWORK_LABEL": args.network_label,
        "PERF_TEST_PROJECT_LABEL": args.project_label,
    }
    if args.base_plan_ids:
        instrument_args["PERF_TEST_BASE_PLAN_IDS"] = args.base_plan_ids
    cmd = ["shell", "am", "instrument", "-w"]
    for key, value in instrument_args.items():
        cmd += ["-e", key, value]
    cmd.append(TEST_RUNNER)
    result = adb(cmd, serial=serial, check=False, capture=True, timeout=args.cell_timeout)
    print(result.stdout)
    if result.stderr:
        print(result.stderr, file=sys.stderr)
    ok = "OK (" in result.stdout and "FAILURES!!!" not in result.stdout
    if not ok:
        print(
            f"WARNING: instrumentation reported failure for {scenario}/{cache_mode}/{lifecycle_mode}",
            file=sys.stderr,
        )
    return ok


def clear_app_data(serial):
    adb(["shell", "pm", "clear", TEST_PACKAGE], serial=serial, check=False)


def pull_results(serial, results_dir):
    results_dir.mkdir(parents=True, exist_ok=True)
    adb(["pull", DEVICE_RESULTS_DIR, str(results_dir)], serial=serial, check=False)


def load_results(results_dir):
    results = []
    for path in sorted(results_dir.rglob("*.json")):
        try:
            with open(path) as f:
                data = json.load(f)
        except (json.JSONDecodeError, OSError) as e:
            print(f"Skipping unreadable result file {path}: {e}", file=sys.stderr)
            continue
        if data.get("schema_version") == 1:
            results.append(data)
    return results


def percentile(sorted_values, pct):
    """Nearest-rank percentile, matching PerfAggregate in the instrumentation harness."""
    if not sorted_values:
        return None
    rank = max(1, min(len(sorted_values), math.ceil(pct / 100.0 * len(sorted_values))))
    return sorted_values[rank - 1]


def fmt(value):
    return f"{value:.0f}" if value is not None else "-"


def aggregate(results):
    """Groups runs by (scenario, cache, lifecycle, network, project) and prints one table per metric."""
    groups = {}
    for result in results:
        key = (
            result["scenario"],
            result["cache_mode"],
            result.get("lifecycle_mode", "foreground"),
            result["network_label"],
            result.get("project_label", "default"),
        )
        groups.setdefault(key, []).extend(result["iterations"])

    metrics = [
        ("get_offerings_ms", "get_offerings duration (headline)"),
        ("configure_to_offerings_ms", "configure -> offerings callback"),
        ("resume_to_offerings_ms", "app resume -> offerings callback"),
        ("ui_config_ms", "ui_config readiness after offerings"),
        ("workflow_body_ms", "first workflow body after offerings"),
    ]

    for metric, title in metrics:
        rows = []
        for key in sorted(groups):
            iterations = groups[key]
            values = sorted(i[metric] for i in iterations if i.get(metric) is not None)
            if not values and metric != "get_offerings_ms":
                continue
            errors = sum(1 for i in iterations if not i.get("success"))
            fallbacks = sum(
                1
                for i in iterations
                if i.get("ui_config_is_default") is True or i.get("workflow_resolved") is False
            )
            rows.append(
                (
                    *key,
                    len(values),
                    fmt(percentile(values, 50)),
                    fmt(percentile(values, 90)),
                    fmt(percentile(values, 95)),
                    fmt(values[-1] if values else None),
                    errors,
                    fallbacks,
                )
            )
        if not rows:
            continue
        header = (
            "scenario",
            "cache",
            "lifecycle",
            "network",
            "project",
            "n",
            "p50",
            "p90",
            "p95",
            "max",
            "errors",
            "fallbacks",
        )
        widths = [max(len(str(r[i])) for r in [header] + rows) for i in range(len(header))]
        print(f"\n== {title} (ms) ==")
        for row in [header] + rows:
            print("  ".join(str(cell).ljust(width) for cell, width in zip(row, widths)))


def cmd_run(args):
    scenarios = ALL_SCENARIOS if args.scenarios == ["all"] else args.scenarios
    cache_modes = ALL_CACHE_MODES if args.cache_modes == ["all"] else args.cache_modes
    lifecycle_modes = ALL_LIFECYCLE_MODES if args.lifecycle_modes == ["all"] else args.lifecycle_modes

    for scenario in scenarios:
        if scenario not in ALL_SCENARIOS:
            sys.exit(f"Unknown scenario '{scenario}'. Valid: {ALL_SCENARIOS}")
    for cache in cache_modes:
        if cache not in ALL_CACHE_MODES:
            sys.exit(f"Unknown cache mode '{cache}'. Valid: {ALL_CACHE_MODES}")
    for lifecycle in lifecycle_modes:
        if lifecycle not in ALL_LIFECYCLE_MODES:
            sys.exit(f"Unknown lifecycle mode '{lifecycle}'. Valid: {ALL_LIFECYCLE_MODES}")

    matrix = [
        (scenario, cache, lifecycle)
        for scenario in scenarios
        for cache in cache_modes
        for lifecycle in lifecycle_modes
        if is_valid_cell(scenario, cache, lifecycle)
    ]
    if not matrix:
        sys.exit("The requested scenario/cache/lifecycle combination contains no valid cells.")

    if not args.skip_build:
        build_apk()
    install_apk(args.serial)

    results_dir = Path(args.results_dir)
    started = time.time()
    for index, (scenario, cache_mode, lifecycle_mode) in enumerate(matrix):
        print(
            f"\n=== [{index + 1}/{len(matrix)}] scenario={scenario} cache={cache_mode} "
            f"lifecycle={lifecycle_mode} ==="
        )
        # Process-level isolation between cells: cold cells start from a truly clean app.
        clear_app_data(args.serial)
        run_cell(args.serial, scenario, cache_mode, lifecycle_mode, args)
        pull_results(args.serial, results_dir)
    print(f"\nMatrix finished in {time.time() - started:.0f}s. Results in {results_dir}/")
    aggregate(load_results(results_dir))


def cmd_aggregate(args):
    results = load_results(Path(args.results_dir))
    if not results:
        sys.exit(f"No results found in {args.results_dir}")
    aggregate(results)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)

    run_parser = sub.add_parser("run", help="Build, install, run the matrix, pull + aggregate results.")
    run_parser.add_argument("--api-key", required=True, help="API key of the perf test project.")
    run_parser.add_argument(
        "--scenarios",
        nargs="+",
        default=["all"],
        help=f"Scenarios to run ({', '.join(ALL_SCENARIOS)} or 'all').",
    )
    run_parser.add_argument(
        "--cache-modes",
        nargs="+",
        default=["all"],
        help=f"Cache modes to run ({', '.join(ALL_CACHE_MODES)} or 'all').",
    )
    run_parser.add_argument(
        "--lifecycle-modes",
        nargs="+",
        default=["all"],
        help=f"Lifecycle modes to run ({', '.join(ALL_LIFECYCLE_MODES)} or 'all'). "
        "Invalid cache/lifecycle combinations are skipped automatically.",
    )
    run_parser.add_argument("--iterations", type=int, default=10, help="Timed iterations per cell.")
    run_parser.add_argument(
        "--network-label",
        default="unlabeled",
        help="Label describing the externally applied network shaping (e.g. ideal, lte, lte_loss10).",
    )
    run_parser.add_argument("--project-label", default="default", help="Label describing the test project size.")
    run_parser.add_argument(
        "--base-plan-ids",
        default="",
        help="Comma-separated base plan ids to fabricate per product (needed for Play Store projects).",
    )
    run_parser.add_argument("--serial", default=None, help="adb device serial (defaults to the only device).")
    run_parser.add_argument("--results-dir", default="perf-results", help="Local directory for pulled results.")
    run_parser.add_argument("--skip-build", action="store_true", help="Reuse the already-built test APK.")
    run_parser.add_argument(
        "--cell-timeout",
        type=int,
        default=3600,
        help="Max seconds per matrix cell before the instrumentation run is killed.",
    )
    run_parser.set_defaults(func=cmd_run)

    aggregate_parser = sub.add_parser("aggregate", help="Aggregate previously pulled result files.")
    aggregate_parser.add_argument("--results-dir", default="perf-results")
    aggregate_parser.set_defaults(func=cmd_aggregate)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
