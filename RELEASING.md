Releasing
=========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. Change the version number in Purchases.java
 2. Change the versionName in purchases/build.gradles
 2. Update the `CHANGELOG.md` for the impending release.
 4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 6. `./gradlew clean uploadArchives`
 7. Update the `gradle.properties` to the next SNAPSHOT version.
 8. `git commit -am "Prepare next development version."`
 9. `git push && git push --tags`
 10. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
 11. Click on Staging Repositories on the left side
 12. Scroll down to find the purchase respostiry
 13. Select and click "Close" from the top menu. Why is it called close?
 14. Once close is complete, repeat but this time selecting "Release"

