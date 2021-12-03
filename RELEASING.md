Automatic Releasing
=========
 1. Create a branch release/x.y.z
 1. Update CHANGELOG.latest.md with the changes for the current version (to be used by Fastlane for the github release notes)
 1. Run `bundle exec fastlane bump_and_update_changelog version:X.Y.Z` (where X.Y.Z is the new version) to update the version number in `gradle.properties`, `Config.kt` and in `library.gradle`
 1. Commit the changes `git commit -am "Version X.Y.Z"` (where X.Y.Z is the new version)
 1. Make a PR into main, merge when approved
 1. Pull main
 1. Make a tag and push, the rest will be performed automatically by CircleCI. If the automation fails, you can revert
 to manually calling `bundle exec fastlane deploy`.

Hotfix Releases
=========
Sometimes not all commits from main should be included in a release. This is a typical scenario
with a hotfix release, where an urgent bugfix needs to be released and some features already merged
are not ready to be released.

1. Open a branch from the latest release (or the last commit before the one that shouldn't be included). Name it release/x.x.x
1. Cherry pick all other commits that should be included.
1. Create a CHANGELOG.latest.md with the changes for the current version (to be used by Fastlane for the github release notes)
1. Run `bundle exec fastlane bump_and_update_changelog version:X.Y.Z` (where X.Y.Z is the new version) to update the version number in `gradle.properties`, `Config.kt` and in `library/build.gradle`
1. Commit the changes `git commit -am "Version X.Y.Z"` (where X.Y.Z is the new version)
1. Make a PR, **don't merge** it when approved. Instead, close the PR.
1. Tag the last commit in that branch and push. The rest of the process should be as in the automatic release.
CircleCI will upload a release to Sonatype.
1. Open a PR against main updating the changelog to include the changes from the hotfix release.