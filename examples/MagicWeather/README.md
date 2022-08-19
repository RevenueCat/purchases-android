# Magic Weather Android - RevenueCat Sample

Magic Weather is a sample app demonstrating the proper methods for using RevenueCat's *Purchases* SDK. This sample uses only native platform components - no third-party SDKs other than the *Purchases* SDK.

Sign up for a free RevenueCat account [here](https://www.revenuecat.com).

## Requirements

This sample uses:

- Android Studio
- Android (API 8.0+)
- Kotlin 1.6.21

See minimum platform version requirements for RevenueCat's *Purchases* SDK [here](https://github.com/RevenueCat/purchases-android/blob/main/examples/MagicWeather/build.gradle).

## Features

| Feature                          | Sample Project Location                   |
| -------------------------------- | ----------------------------------------- |
| 🕹 Configuring the *Purchases* SDK  | [MainActivity.kt](app/src/main/java/com/revenuecat/sample/MainActivity.kt) |
| 💰 Building a basic paywall         | [paywall/PaywallFragment.kt](app/src/main/java/com/revenuecat/sample/ui/paywall/PaywallFragment.kt) |
| 🔐 Checking subscription status     | [weather/WeatherFragment.kt](app/src/main/java/com/revenuecat/sample/ui/weather/WeatherFragment.kt#L69) |
| 🤑 Restoring transactions           | [user/UserFragment.kt](app/src/main/java/com/revenuecat/sample/ui/user/UserFragment.kt#L81) |
| 👥 Identifying the user             | [user/UserFragment.kt](app/src/main/java/com/revenuecat/sample/ui/user/UserFragment.kt#L97) |
| 🚪 Logging out the user             | [user/UserFragment.kt](app/src/main/java/com/revenuecat/sample/ui/user/UserFragment.kt#L97) |

## Setup & Run

### Prerequisites
- Be sure to have a [Google Play Console Account](https://play.google.com/console/developers).
- Be sure to set up at least one subscription on the Play Store following our [guide](https://docs.revenuecat.com/docs/google-play-store) and link it to RevenueCat:
    - Add the [product](https://docs.revenuecat.com/docs/entitlements#products) (e.g. `rc_3999_1y`) to RevenueCat's dashboard. It should match the product ID on the Play Store.
    - Attach the product to an [entitlement](https://docs.revenuecat.com/docs/entitlements#creating-an-entitlement), e.g. `premium`.
    - Attach the product to a [package](https://docs.revenuecat.com/docs/entitlements#adding-packages) (e.g. `Annual`) inside an [offering](https://docs.revenuecat.com/docs/entitlements#creating-an-offering) (e.g. `sale` or `default`).
- Get your [API key](https://docs.revenuecat.com/docs/authentication#obtaining-api-keys) from your RevenueCat project.

### Steps to Run
1. Download or clone this repository
    >git clone https://github.com/RevenueCat/purchases-android.git

2. Open the MagicWeather project in Android Studio

    <img src="https://i.imgur.com/dDSod4g.png" alt="Android Studio open file navigation" width="250px" />

3. In `build.gradle`, match `applicationId` to your Google Play package in Google Play Console and RevenueCat.
    
    <img src="https://i.imgur.com/1iI5MaA.png" alt="Build Gradle with applicationId" width="250px" />

4. In the `Constants.kt` file: 
    - Replace the value for `GOOGLE_API_KEY` with the API key from your RevenueCat project.
    - Replace the value for `AMAZON_API_KEY` with the API key from your RevenueCat project (if applicable).
    - Replace the value for `entitlementID` with the entitlement ID of your product in RevenueCat's dashboard.

    <img src="https://i.imgur.com/LXsH3tL.png" alt="Constants.kt file" width="250px" />

5. Run the app on a simulator or physical device.

    <img src="https://i.imgur.com/GlazHU5.png" alt="Run Android Studio project" width="250px" />
    <img src="https://i.imgur.com/lGQYmKK.png" alt="Run Android Studio project" width="180px" />


### Example Flow: Purchasing a Subscription

1. On the home page, select **Change the Weather**.
2. On the prompted payment sheet, select the product listed.
3. On the next modal, select **Subscribe**.
4. On the next modal, sign in with your Sandbox User ID.
5. On the next modal, select **Ok**.
6. Return to the home page and select **Change the Weather** to see the weather change!

#### Purchase Flow Demo (`iOS` version)
<img src="https://i.imgur.com/SSbRLhr.gif" width="220px" />

## Support

For more technical resources, check out our [documentation](https://docs.revenuecat.com).

Looking for RevenueCat Support? Visit our [community](https://community.revenuecat.com/).