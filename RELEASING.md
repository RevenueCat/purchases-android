Releasing
=========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 1. Change the version number in Purchases.kt
 1. Change the versionName in purchases/build.gradle.
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
