# Legacy AdMob compatibility app

This standalone Android project verifies that the current `purchases-admob` artifact can be consumed by an app
using the latest supported Google Mobile Ads SDK (Legacy). It is a compile-and-package fixture, not a runnable
integration sample. For a runnable example, use [`examples/admob-sample`](../../examples/admob-sample).

The project is intentionally excluded from the repository's root `settings.gradle.kts`. Google Mobile Ads SDK 25.x
contains Kotlin metadata that requires a newer Kotlin compiler than the main Purchases Android build currently uses.
Keeping this app in a separate Gradle build lets it use Kotlin 2.2 while the published RevenueCat artifacts retain
their existing Kotlin compatibility. The fixture uses minSdk 24 because that is the minimum declared by Google
Mobile Ads SDK 25.x; it does not change the minimum supported by the RevenueCat SDK or adapter.

## Run locally

Publish the current SDK artifacts to a temporary Maven repository, then build the compatibility app against them:

```bash
repository_path="$PWD/build/admob-legacy-compatibility-m2"
revenuecat_version="$(sed -n 's/^VERSION_NAME=//p' gradle.properties)"

./gradlew \
    :purchases:publishToMavenLocal \
    :feature:admob:publishToMavenLocal \
    -Dmaven.repo.local="$repository_path" \
    --no-daemon

./gradlew \
    -p test-apps/admob-legacy-compatibility \
    :app:assembleDebug \
    -Dmaven.repo.local="$repository_path" \
    -PrevenueCatVersion="$revenuecat_version" \
    --no-daemon
```

The app directly depends on the pinned Google Mobile Ads SDK version in its own version catalog. Gradle resolves
that version instead of the older transitive version declared by `purchases-admob`. Renovate should update the pin so
CI verifies new Google releases explicitly rather than using a non-reproducible dynamic version.
