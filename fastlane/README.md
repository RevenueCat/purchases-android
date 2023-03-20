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

### android build_purchase_tester_bundle

```sh
[bundle exec] fastlane android build_purchase_tester_bundle
```

Build purchase tester app bundle

### android publish_purchase_tester

```sh
[bundle exec] fastlane android publish_purchase_tester
```

Publish purchase tester to test track in Play Console

### android build_example

```sh
[bundle exec] fastlane android build_example
```

Builds a Magic Weather APK and prompts for:

* Gradle task

* Amazon or Google API Key for RevenueCat

* Version code

* Version name

* Amazon pem path (optional)

* New application id (optional)

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
