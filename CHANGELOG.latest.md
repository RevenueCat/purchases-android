## RevenueCat SDK

> [!WARNING]
> Your app, and any library module that depends on the SDK, must now compile against API 35 or later. This requirement comes from `androidx.core` 1.15.0, which Play Billing Library 9 depends on. `minSdk` and `targetSdk` are not affected.

### 💥 Breaking Changes
This release updates the SDK to Google Play Billing Library 9.1.0. The RevenueCat API is unchanged, but the new dependencies raise the build requirements of your app. You can check the full list of Billing Library changes here: https://developer.android.com/google/play/billing/release-notes#9-0-0

We have also updated Kotlin to 2.2.21, Jetpack Compose to BOM 2026.06.01, kotlinx-coroutines to 1.9.0 and kotlinx-serialization to 1.7.3. The minimum Android version stays at Android 6 (API 23).

See the [v11 migration guide](https://github.com/RevenueCat/purchases-android/blob/main/migrations/v11-MIGRATION.md) for details.

* Update to Play Billing Library 9.1.0 (#4118) via @Climbatize (#4192) via Álvaro Brey (@AlvaroBrey)
* Update to AGP 9 (AGP 9.2.1, Gradle 9.4.1, Kotlin 2.2.21) (#3964) via Álvaro Brey (@AlvaroBrey)
### 🐞 Bugfixes
* Fix paywall image localization parsing under kotlinx-serialization 1.7.3 (#4258) via Álvaro Brey (@AlvaroBrey)
### 📦 Dependency Updates
* Update kotlinx-coroutines to 1.9.0 (#4261) via Álvaro Brey (@AlvaroBrey)

## RevenueCatUI SDK
### 💥 Breaking Changes
* Update Compose to BOM 2026.06.01 (#4253) via Álvaro Brey (@AlvaroBrey)

### 🔄 Other Changes
* Complete the v11 migration guide (#4439) via Álvaro Brey (@AlvaroBrey)
* Fix 11.0-dev test failures from the kotlinx-serialization and Compose bumps (#4432) via Álvaro Brey (@AlvaroBrey)
* Move sdksizetesting to AGP 9 (#4128) via Álvaro Brey (@AlvaroBrey)
* Drop the AGP 9 built-in Kotlin opt-out (#4120) via Álvaro Brey (@AlvaroBrey)
* Move MagicWeatherCompose and CustomEntitlementComputationSample to AGP 9 (#4115) via Álvaro Brey (@AlvaroBrey)
