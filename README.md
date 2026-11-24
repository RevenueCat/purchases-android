# purchases-android

The RevenueCat SDK for Android. It wraps Google Play Billing and handles purchases, entitlements, and subscription status.

[![Maven Central version](https://img.shields.io/maven-central/v/com.revenuecat.purchases/purchases)](https://central.sonatype.com/artifact/com.revenuecat.purchases/purchases) [![License: MIT](https://img.shields.io/github/license/RevenueCat/purchases-android)](https://github.com/RevenueCat/purchases-android/blob/main/LICENSE)

[Docs](https://www.revenuecat.com/docs?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android) · [Install guide](https://www.revenuecat.com/docs/getting-started/installation/android?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android) · [SDK reference](https://sdk.revenuecat.com/android/index.html) · [Sample app](https://github.com/RevenueCat/purchases-android/tree/main/examples/MagicWeatherCompose) · [Changelog](https://github.com/RevenueCat/purchases-android/releases) · [Migration guide](https://www.revenuecat.com/docs/sdk-guides/android-native-10x-to-11x-migration?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android)

## Requirements

| Requirement | Minimum |
|---|---|
| Android | 6.0 (API level 23) |
| Compile SDK | API 35 |
| Kotlin | 2.1.0 |
| Java source and target compatibility | 8 |

Paywalls and Customer Center (`purchases-ui`) need Android 7.0 (API level 24) or later.

## Installation

Add the SDK from Maven Central to your module's `build.gradle.kts`. Leave out `purchases-ui` if you don't show Paywalls or Customer Center; it needs Android 7.0 (API level 24).

```kotlin
implementation("com.revenuecat.purchases:purchases:<latest version>")
implementation("com.revenuecat.purchases:purchases-ui:<latest version>")
```

Replace `<latest version>` with the version on the Maven Central badge above.

Then, in `AndroidManifest.xml`, set the `launchMode` of the Activity that triggers purchases to `standard` or `singleTop`. Other modes can cancel the purchase if Google Play sends the user to another app, such as their banking app, to verify payment. See the [launchMode step](https://www.revenuecat.com/docs/getting-started/installation/android?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android#3-set-your-activitys-launchmode).

- Version catalog: declare the same dependencies in `gradle/libs.versions.toml`. See the [install guide](https://www.revenuecat.com/docs/getting-started/installation/android?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android#1-install-the-sdk).
- Amazon Appstore or Galaxy Store: add the store module from the table below, then follow the [other stores steps](https://www.revenuecat.com/docs/getting-started/installation/android?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android#other-stores). Amazon builds configure with `AmazonConfiguration.Builder`; see [Configure the SDK](https://www.revenuecat.com/docs/getting-started/configuring-sdk?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android#initialization).

| Package | What it's for | When you need it |
|---|---|---|
| `purchases` | Purchases, entitlements, subscription status | Always |
| `purchases-ui` | Paywalls and Customer Center | You show RevenueCat Paywalls or Customer Center. Needs Android 7.0 (API level 24) |
| `purchases-store-amazon` | Amazon Appstore purchases | You sell through the Amazon Appstore |
| `purchases-store-galaxy` | Galaxy Store purchases | You sell through the Galaxy Store |
| `purchases-admob` | Tracks AdMob ad events in RevenueCat | You show AdMob ads. Apps on the Google Mobile Ads Next-Gen SDK use `purchases-admob-next-gen` instead (Android 7.0, API level 24) |

## Quick start

Before you start, create a RevenueCat project, connect your store or start with the Test Store, and add an entitlement. See the [Quickstart](https://www.revenuecat.com/docs/getting-started/quickstart?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android).

Configure `Purchases` once in your `Application` class with your public API key (the Test Store key while you test):

```kotlin
import android.app.Application
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(PurchasesConfiguration.Builder(this, <public_google_api_key>).build())
    }
}
```

If you add `MainApplication` as shown above, set `android:name=".MainApplication"` on the `<application>` element in `AndroidManifest.xml`. If your app already has an `Application` subclass, call `Purchases.configure` in its `onCreate()` instead.

Check whether the user has an active entitlement before you show a paywall:

```kotlin
import com.revenuecat.purchases.getCustomerInfoWith

Purchases.sharedInstance.getCustomerInfoWith(
    onError = { error -> /* Optional error handling */ },
    onSuccess = { customerInfo ->
        if (customerInfo.entitlements[<my_entitlement_identifier>]?.isActive == true) {
            // Grant user "pro" access
        }
    }
)
```

## What's included

- [Paywalls](https://www.revenuecat.com/docs/tools/paywalls?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): build single-screen or multi-step paywalls in the dashboard and change them without an app update.
- [Customer Center](https://www.revenuecat.com/docs/tools/customer-center?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): a self-service screen where users cancel subscriptions, restore purchases, and contact your support team.
- [Placements](https://www.revenuecat.com/docs/tools/targeting/placements?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): serve a different Offering at each paywall location in your app.
- [Test Store](https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): test purchases before you set up Google Play.
- [In-app currency](https://www.revenuecat.com/docs/offerings/in-app-currency?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): read the current Customer's in-app currency balances.
- [Redemption Links](https://www.revenuecat.com/docs/web/redemption-links?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): unlock a web purchase in your app from a deep link.
- [Your own purchase code](https://www.revenuecat.com/docs/migrating-to-revenuecat/sdk-or-not/finishing-transactions?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): set `purchasesAreCompletedBy` to keep completing purchases with your existing billing code.
- [Offline entitlements](https://www.revenuecat.com/docs/customers/customer-info?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android#offline-entitlements): the SDK can grant entitlements for new subscription purchases while RevenueCat's servers are down. This feature is unavailable in Test Store and when your app completes purchases itself.
- [Trusted Entitlements](https://www.revenuecat.com/docs/customers/trusted-entitlements?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): the SDK checks a signature on entitlement data so your app can detect tampered responses. It runs in informational mode by default: it reports the result and doesn't block access.
- [Amazon Appstore and Galaxy Store](https://www.revenuecat.com/docs/getting-started/installation/android?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android#other-stores): sell outside Google Play with the `purchases-store-amazon` or `purchases-store-galaxy` module.
- [Ad Monetization](https://www.revenuecat.com/docs/ad-monetization?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): report ad impressions, clicks, and revenue next to subscription data. The [AdMob adapter](https://www.revenuecat.com/docs/getting-started/adapter-sdks/admob?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android) tracks AdMob events automatically (Kotlin only).

## Documentation

- [Install guide](https://www.revenuecat.com/docs/getting-started/installation/android?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): Gradle and version catalog setup, Activity `launchMode`, and the Amazon Appstore and Galaxy Store modules.
- [Configure the SDK](https://www.revenuecat.com/docs/getting-started/configuring-sdk?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): initialization, debug logs, Test Store, and other configuration options.
- [Displaying paywalls](https://www.revenuecat.com/docs/tools/paywalls/displaying-paywalls?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): present a paywall from Jetpack Compose or an Activity.
- [Testing purchases](https://www.revenuecat.com/docs/test-and-launch/sandbox?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): Test Store and sandbox testing.
- [Migration guide](https://www.revenuecat.com/docs/sdk-guides/android-native-10x-to-11x-migration?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): changes in the current major version.
- [SDK reference](https://sdk.revenuecat.com/android/index.html): every public class and method.

## Using an AI coding agent

Your agent's training data is likely older than the current version of this SDK. Point it at current sources:

- [llms.txt](https://www.revenuecat.com/docs/llms.txt?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): an index of the docs. Add `.md` to any docs URL for a Markdown version.
- [RevenueCat MCP server](https://www.revenuecat.com/docs/tools/mcp?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android): lets your agent read and change your RevenueCat project configuration.
- [AI Toolkit](https://github.com/RevenueCat/ai-toolkit): installs RevenueCat skills and the MCP server in one step.
- [Releases](https://github.com/RevenueCat/purchases-android/releases): check the latest version before adding the dependency.

## Sample apps

- [MagicWeatherCompose](https://github.com/RevenueCat/purchases-android/tree/main/examples/MagicWeatherCompose): a Jetpack Compose app that configures the SDK for Google Play or the Amazon Appstore, builds a custom paywall from Offerings, checks subscription status, restores purchases, and logs users in.

## Getting help

- Bugs: open an issue in [this repo](https://github.com/RevenueCat/purchases-android/issues).
- Account or configuration questions: [RevenueCat Support](https://www.revenuecat.com/docs/revenuecat-support/support-first-steps?utm_source=github&utm_medium=sdk_readme&utm_content=purchases-android).
- Discussion: [RevenueCat Community](https://community.revenuecat.com/).

## Contributing

This repository is published from the `main` branch and release tags of RevenueCat's internal development repository. Issues are tracked here. Pull requests are welcome: a maintainer reviews them here and brings accepted changes into the internal repository, so they can't be merged directly.

See the [contributing guide](https://github.com/RevenueCat/purchases-android/blob/main/CONTRIBUTING.md).

## License

MIT. See [LICENSE](https://github.com/RevenueCat/purchases-android/blob/main/LICENSE).
