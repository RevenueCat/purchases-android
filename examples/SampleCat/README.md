# SampleCat Android

A sample Android app demonstrating RevenueCat SDK integration to help developers and allow easy debugging of issues.
It is built with Jetpack Compose and Material 3.

## Features

- **Offerings** - Browse available RevenueCat offerings and packages
- **Products** - View all products across offerings with purchase capability
- **Paywalls** - Display RevenueCat's pre-built paywall UI for any offering
- **Customer Center** - Self-service subscription management for customers

## Configuration

| Property | Required | Description |
|----------|----------|-------------|
| `REVENUECAT_API_KEY` | Yes | Google Play API key from RevenueCat dashboard |
| `APPLICATION_ID` | Yes | Package name matching your Play Console app |
| `REVENUECAT_ENTITLEMENT_ID` | No | Entitlement identifier for subscription status |

Open `gradle.properties` and set your values:

```properties
# Required: Your RevenueCat Google Play API key
REVENUECAT_API_KEY=goog_xxxxxxxxxxxxx

# Required: Your app's package name (must match Play Console)
APPLICATION_ID=com.yourcompany.yourapp

# Optional: Entitlement ID to check subscription status
REVENUECAT_ENTITLEMENT_ID=premium
```

The build will fail with a helpful error message if required properties are not configured.

## Architecture

- **UI**: Jetpack Compose with Material 3
- **State Management**: ViewModel with StateFlow
- **Navigation**: Navigation Compose with bottom navigation
- **Async**: Kotlin Coroutines with RevenueCat suspend functions

## Roadmap

The app is almost at parity with its [iOS counterpart](https://github.com/RevenueCat/purchases-ios/tree/main/Examples/SampleCat).
The only aspect left is the health check ability that relies on fetching a health report with the SDK.
This method, `PurchasesDiagnostics.default.healthReport()`, currently does not exist in the Android SDK.
It should be added first in the SDK, before we can implement the health checks in this sample app.