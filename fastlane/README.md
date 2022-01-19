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

Increment build number

### android bump_and_update_changelog

```sh
[bundle exec] fastlane android bump_and_update_changelog
```

Increment build number and update changelog

### android tag_release_with_latest_if_needed

```sh
[bundle exec] fastlane android tag_release_with_latest_if_needed
```

Tag release version with latest if necessary

### android github_release

```sh
[bundle exec] fastlane android github_release
```

Make github release

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

Prepare next version

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
