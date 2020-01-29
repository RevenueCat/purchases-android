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
or alternatively using `brew cask install fastlane`

# Available Actions
### github_release
```
fastlane github_release
```
Make github release

----

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

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
