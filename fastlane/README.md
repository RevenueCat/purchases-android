fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

### android setup_dev

```sh
[bundle exec] fastlane android setup_dev
```

Setup development environment

### android verify_debug_view_snapshot_tests

```sh
[bundle exec] fastlane android verify_debug_view_snapshot_tests
```

Verify snapshot tests for the debug view library

### android record_debug_view_snapshot_tests

```sh
[bundle exec] fastlane android record_debug_view_snapshot_tests
```

Record/Update snapshots for tests in the debug view library

### android bump

```sh
[bundle exec] fastlane android bump
```

Replaces version numbers, updates changelog and creates PR

### android automatic_bump

```sh
[bundle exec] fastlane android automatic_bump
```

Automatically bumps version, replaces version numbers, updates changelog and creates PR

### android github_release

```sh
[bundle exec] fastlane android github_release
```

Creates github release

### android deploy

```sh
[bundle exec] fastlane android deploy
```

Upload and close a release

### android deploy_snapshot

```sh
[bundle exec] fastlane android deploy_snapshot
```

Upload a snapshot release

### android prepare_next_version

```sh
[bundle exec] fastlane android prepare_next_version
```

Creates PR changing version to next minor adding a -SNAPSHOT suffix

### android tag_current_branch

```sh
[bundle exec] fastlane android tag_current_branch
```

Tag current branch with current version number

### android build_bundle

```sh
[bundle exec] fastlane android build_bundle
```

Build app bundle

### android build_purchase_tester_bundle

```sh
[bundle exec] fastlane android build_purchase_tester_bundle
```

Build purchase tester app bundle

### android build_paywall_tester_bundle

```sh
[bundle exec] fastlane android build_paywall_tester_bundle
```

Build paywall tester app bundle

### android build_default_purchases_integration_tests

```sh
[bundle exec] fastlane android build_default_purchases_integration_tests
```

Build purchases module integration tests pointing to production

### android run_backend_integration_tests

```sh
[bundle exec] fastlane android run_backend_integration_tests
```

Build purchases module integration tests pointing to production

### android run_load_shedder_purchases_integration_tests

```sh
[bundle exec] fastlane android run_load_shedder_purchases_integration_tests
```

Build and run purchases module load shedder integration tests

This requires the google cloud cli to be installed and initialized.

### android run_custom_entitlement_computation_integration_tests

```sh
[bundle exec] fastlane android run_custom_entitlement_computation_integration_tests
```

Build and run purchases module custom entitlement computation integration tests

This requires the google cloud cli to be installed and initialized.

### android publish_to_test_track

```sh
[bundle exec] fastlane android publish_to_test_track
```

Publish to test track in Play Console

### android publish_purchase_tester

```sh
[bundle exec] fastlane android publish_purchase_tester
```

Publish purchase tester to test track in Play Console

### android publish_paywall_tester

```sh
[bundle exec] fastlane android publish_paywall_tester
```

Publish paywall tester to test track in Play Console

### android build_magic_weather_compose

```sh
[bundle exec] fastlane android build_magic_weather_compose
```

Builds a Magic Weather Compose APK

### android build_custom_entitlement_computation_sample

```sh
[bundle exec] fastlane android build_custom_entitlement_computation_sample
```

Builds a Magic Weather Compose APK

### android build_magic_weather

```sh
[bundle exec] fastlane android build_magic_weather
```

Builds a Magic Weather APK and prompts for:
* Gradle task
* Amazon or Google API Key for RevenueCat
* Version code
* Version name
* Amazon pem path (optional)
* New application id (optional)


### android build_purchase_tester

```sh
[bundle exec] fastlane android build_purchase_tester
```

Builds a Purchase Tester APK and prompts for:
* Version code
* Version name
* Min SDK Version
* Amazon pem path (optional)


### android send_slack_load_shedder_integration_test

```sh
[bundle exec] fastlane android send_slack_load_shedder_integration_test
```



### android fetch_snapshots

```sh
[bundle exec] fastlane android fetch_snapshots
```

Clones or updates snapshots repo

### android verify_revenuecatui_snapshot_tests

```sh
[bundle exec] fastlane android verify_revenuecatui_snapshot_tests
```

Verify RevenueCat UI snapshots

### android record_revenuecatui_snapshot_tests

```sh
[bundle exec] fastlane android record_revenuecatui_snapshot_tests
```

Record RevenueCat UI snapshots

### android create_snapshots_repo_pr

```sh
[bundle exec] fastlane android create_snapshots_repo_pr
```

Creates a new PR on purchases-ios-snapshots after new snapshot files were generated

### android generate_snapshots_RCUI

```sh
[bundle exec] fastlane android generate_snapshots_RCUI
```

Trigger CircleCI job to generate snapshots for RevenueCatUI

### android update_snapshots_repo

```sh
[bundle exec] fastlane android update_snapshots_repo
```

Updates purchases-ios-snapshots-commit to point to latest commit on main

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
