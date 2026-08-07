# Unify Maestro RevenueCat Project Configuration

## Goal

Run both Android Maestro suites against the RevenueCat project currently used by the workflow tests. The project is selected through one neutral CircleCI environment variable, `MAESTRO_TEST_STORE_API_KEY`, without exposing its value in the repository or disrupting pull requests that still use the previous CI configuration.

## Current State

The two Maestro jobs use different RevenueCat projects and different key-injection mechanisms:

- `run-maestro-e2e-tests` replaces `Constants.API_KEY` in a Kotlin source file with `TEST_STORE_API_KEY` before building.
- `run-maestro-workflow-tests` passes `WORKFLOWS_TEST_STORE_API_KEY` as the `E2E_WORKFLOWS_API_KEY` Gradle property, which becomes `BuildConfig.WORKFLOWS_API_KEY`.

The workflow build also changes Purchases initialization timing. It waits for the first Activity so Maestro launch arguments can configure workflow failure strategies and locale before the SDK starts. The legacy annual-purchase flow configures Purchases eagerly in `Application.onCreate`.

## Design

### Shared API-key input

Both CircleCI Maestro jobs will read `MAESTRO_TEST_STORE_API_KEY` from the existing `maestro` context and pass it to Gradle as `E2E_API_KEY`. The variable has already been created with the same value as `WORKFLOWS_TEST_STORE_API_KEY`, so both jobs will target the retained workflow RevenueCat project.

The E2E app will expose the resolved value as `BuildConfig.API_KEY`. `Constants.API_KEY`, the source-file `sed` replacement, `E2E_WORKFLOWS_API_KEY`, and `BuildConfig.WORKFLOWS_API_KEY` will be removed.

The local-development property will be renamed from `E2E_WORKFLOWS_API_KEY` to `E2E_API_KEY` in `local.properties.example`. A placeholder remains the default so developers can build without a secret.

### Preserve workflow-specific initialization

Project selection and workflow test behavior will be independent settings:

- `E2E_API_KEY` selects the RevenueCat project for every E2E build.
- `E2E_ENABLE_WORKFLOW_TESTING` controls whether Purchases configuration is deferred until the first Activity.

Gradle will surface the second property as `BuildConfig.ENABLE_WORKFLOW_TESTING`, defaulting to `false`. The workflow CI job will pass it as `true`; the annual-purchase job will use the default. This preserves both suites' existing initialization behavior while eliminating the project-specific conditional.

`E2ETestsApplication` will always use `BuildConfig.API_KEY`. It will choose eager or deferred initialization solely through `BuildConfig.ENABLE_WORKFLOW_TESTING`.

### CI rollout and compatibility

Both Maestro workflow invocations will receive the `maestro` CircleCI context so they can read `MAESTRO_TEST_STORE_API_KEY`. Existing CircleCI variables `TEST_STORE_API_KEY` and `WORKFLOWS_TEST_STORE_API_KEY` will not be deleted as part of this change. Branches based on older revisions will therefore continue using their expected variables.

After this change merges and dependent pull requests have rebased, the obsolete variables can be removed from CircleCI in a separate administrative cleanup.

## Data Flow

1. CircleCI exposes `MAESTRO_TEST_STORE_API_KEY` to either Maestro job.
2. The job invokes Gradle with `-PE2E_API_KEY=$MAESTRO_TEST_STORE_API_KEY`.
3. Gradle writes the value into `BuildConfig.API_KEY`.
4. `E2ETestsApplication` configures Purchases with `BuildConfig.API_KEY`.
5. The workflow job additionally passes `-PE2E_ENABLE_WORKFLOW_TESTING=true`, enabling deferred configuration and Maestro launch-argument handling.

Local builds follow the same path by reading `E2E_API_KEY` and, when needed, `E2E_ENABLE_WORKFLOW_TESTING` from Gradle properties or `local.properties`.

## Failure Handling

- A missing API key resolves to a non-secret placeholder. CI tests will then fail when the app attempts to contact RevenueCat, making a missing context assignment visible without leaking credentials.
- The new CircleCI variable must be present before the updated jobs run. It has already been created and set to the retained workflow-project key.
- The annual purchase flow is the migration check for dashboard parity. Its assertions verify that the retained project exposes the `default` offering, `test_yearly` product, and `pro_cat` entitlement expected by the test.
- Workflow launch arguments continue to be applied before Purchases configuration because the workflow-mode flag preserves deferred initialization.

## Verification

The implementation will be verified in layers:

1. Build the E2E app in default mode with a placeholder or non-secret test value and confirm the generated configuration compiles.
2. Build the E2E app with workflow testing enabled and confirm that mode compiles.
3. Search the repository to confirm production references to `Constants.API_KEY`, `E2E_WORKFLOWS_API_KEY`, `WORKFLOWS_TEST_STORE_API_KEY`, and `TEST_STORE_API_KEY` have been removed from the two Maestro job paths.
4. Run `run-maestro-e2e-tests` in CI to validate the annual test-store purchase against the retained project.
5. Run `run-maestro-workflow-tests` in CI to validate the workflow offering and workflow-specific launch behavior against the same project.

Success means both jobs pass while consuming `MAESTRO_TEST_STORE_API_KEY`, and no API-key value is committed to source control or printed by new logging.

## Out of Scope

- Deleting either old CircleCI environment variable.
- Deleting the old RevenueCat project.
- Changing Maestro test assertions or RevenueCat dashboard configuration beyond ensuring the retained project contains the fixtures already required by both suites.
