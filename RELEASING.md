Automatic Releasing using CircleCI
=========
For an almost fully automated release process, use the trigger pipeline feature in CircleCI passing name `action` and value `bump`

![Screenshot!](https://user-images.githubusercontent.com/664544/191806930-07737e3d-8c44-4bfd-8cef-b471b72643a4.png "CircleCI screenshot")

You can achieve the same using CircleCI API:

```
curl --location --request POST 'https://circleci.com/api/v2/project/github/RevenueCat/purchases-android/pipeline' \
            --header 'Content-Type: application/json' \
            -u "your_circleci_personal_token:" \
            -d '{
              "branch": "main",
              "parameters": {
                "action": "bump"
              }
            }'
```

More info on triggering pipelines in [CircleCI docs](https://circleci.com/docs/triggers-overview).

Automatic Releasing using your machine
=========
1. Create a `fastlane/.env` file with your GitHub API token (see `fastlane/.env.SAMPLE`). This will be used to create the PR, so you should use your own token so the PR gets assigned to you.
2. Run `bundle exec fastlane android bump`
 1. Confirm base branch is correct
 2. Input new version number
 3. Update CHANGELOG.latest.md to include the latest changes. Call out API changes (if any). You can use the existing CHANGELOG.md as a base for formatting. To compile the changelog, you can compare the changes between the base branch for the release (usually main) against the latest release, by checking https://github.com/revenuecat/purchases-android/compare/<latest_release>...<base_branch>. For example, https://github.com/revenuecat/purchases-android/compare/5.1.1...main.
 4. A new branch and PR will automatically be created
3. Get the PR approved (don't merge it). Once it is approved and the release branch tests pass, CircleCI tags the version: the `wait-for-pr-approval` job is approved automatically by the `Approve release hold` GitHub workflow. If that does not happen, approve the job in CircleCI by hand, or tag the last commit in the release branch and push the tag.
4. The rest will be performed automatically by CircleCI, including merging the release PR. If the automation fails, you can revert to manually calling `bundle exec fastlane android deploy`.
5. Merge the bump to the next snapshot version PR right after.

Hotfix Releases
=========
To release a fix merged into `main` without releasing the rest of `main`, for example 3.9.1 when the latest is 4.0.0, or 4.0.1 while `main` is not ready to release:

1. Comment `@RCGitBot hotfix 3` on the merged fix PR. The bot cherry-picks the fix onto the latest 3.x tag, bumps the version, opens a `release/3.9.1` PR into a new `hotfix/3.9.0` branch and comments the release PR link on the fix PR.
1. Approve the release PR, but **don't merge it**. Once tests pass, CircleCI tags 3.9.1, deploys it and merges the PR into `hotfix/3.9.0`. If that does not happen, approve the `wait-for-pr-approval` job in CircleCI by hand, or tag the last commit of `release/3.9.1` with `3.9.1` and push the tag.
1. Delete the `hotfix/3.9.0` branch and add the release to CHANGELOG.md on `main`.

If the cherry-pick conflicts, the bot comments a local command instead: run it from `main`, resolve the conflicts, run `git cherry-pick --continue`, then run it again to open the PR.

To patch an older minor (3.8.x while 3.9.0 exists), run `bundle exec fastlane android hotfix pr:<number> version:3.8.2` locally (releases 3.8.3). Both local commands need `fastlane/.env`, like `bump`.

Fixing timeouts when closing repository
=========
Sonatype might fail when performing the `closeAndRelease` step. Fortunately, we can close and release manually if the publish task succeeds.
- Head to https://central.sonatype.com/publishing/deployments, login using the credentials in 1Password
- You should see one staging repository there. If there are more than one, drop all of them and rerun the failing job in CircleCI.
- Select the only repository available, then do Close. When close finishes, do Release and automatically drop the repository.
