fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

### bump

```sh
[bundle exec] fastlane bump
```

Bump version, edit changelog, and create pull request

### prepare_next_version

```sh
[bundle exec] fastlane prepare_next_version
```

Prepare next version

### github_release

```sh
[bundle exec] fastlane github_release
```

Make github release

### github_changelog

```sh
[bundle exec] fastlane github_changelog
```

Generate changelog from GitHub compare and PR data for mentioning GitHub usernames in release notes

### bump_and_update_changelog

```sh
[bundle exec] fastlane bump_and_update_changelog
```

Increment build number and update changelog

### replace_version_number

```sh
[bundle exec] fastlane replace_version_number
```

Replace version number in project

----


## Android

### android test

```sh
[bundle exec] fastlane android test
```

Runs all the tests

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

### android build_magic_weather

```sh
[bundle exec] fastlane android build_magic_weather
```

Builds a Magic Weather APK and prompts for:

* Gralde task

* Amazon or Google API Key for RevenueCat

* Version code

* Version name

* Amazon pem path (optional)

* New application id (optional)

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
