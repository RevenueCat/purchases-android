fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android test
```
fastlane android test
```
Runs all the tests
### android bump
```
fastlane android bump
```
Increment build number
### android bump_and_update_changelog
```
fastlane android bump_and_update_changelog
```
Increment build number and update changelog
### android github_release
```
fastlane android github_release
```
Make github release
### android deploy
```
fastlane android deploy
```
Upload and close a release
### android deploy_snapshot
```
fastlane android deploy_snapshot
```
Upload a snapshot release
### android prepare_next_version
```
fastlane android prepare_next_version
```
Prepare next version

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
