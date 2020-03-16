Automatic Releasing
=========
 1. Create a branch bump/x.y.z
 1. Create a CHANGELOG.latest.md with the changes for the current version (to be used by Fastlane for the github release notes)
 1. Run `fastlane bump_and_update_changelog version:X.Y.Z` (where X.Y.Z is the new version) to update the version number in `gradle.properties`, `Purchases.kt` and in `purchases/build.gradle` 
 1. Commit the changes `git commit -am "Version X.Y.Z"` (where X.Y.Z is the new version)
 1. Make a PR, merge when approved
 1. Pull master
 1. cd bin
 1. `./release_version.sh -c x.y.z -n a.b.c`, where a.b.c will be the next release after this one. If you're releasing version 3.0.2, for example, this would be ./release_version.sh -c 3.0.2 -n 3.1.0. This will do all of the other steps in the manual process.
 1. Visit [Sonatype Nexus](https://oss.sonatype.org/)
 1. Click on Staging Repositories on the left side
 1. Scroll down to find the purchase repository
 1. Select and click "Close" from the top menu. Why is it called close?
 1. Once close is complete, repeat but this time selecting "Release"
 1. Make a PR for the snapshot bump, merge when approved
 1. Update the version in the Quickstart guide https://docs.revenuecat.com/docs/android

Manual Releasing
=========
 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 1. Change the version number in `Purchases.kt`
 1. Change the versionName in `purchases/build.gradle`.
 1. Update the `CHANGELOG.md` for the impending release.
 1. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 1. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 1. `git push && git push --tags`
 1. Visit [Sonatype Nexus](https://oss.sonatype.org/).
 1. Click on Staging Repositories on the left side
 1. Scroll down to find the purchase repository
 1. Select and click "Close" from the top menu. Why is it called close?
 1. Once close is complete, repeat but this time selecting "Release"
 1. Update the `gradle.properties` to the next SNAPSHOT version.
 1. Change the version number in Purchases.kt
 1. Change the versionName in purchases/build.gradle.
 1. `git commit -am "Prepare next development version."`
 1. `git push`
 1. Update the version in the Quickstart guide https://docs.revenuecat.com/docs/android
 1. Create a Release in GitHub
