<h3 align="center">😻 In-App Subscriptions Made Easy 😻</h3>

[![License](https://img.shields.io/github/license/RevenueCat/purchases-android.svg?style=flat)](https://github.com/RevenueCat/purchases-android/blob/main/LICENSE)
[![Release](https://img.shields.io/github/release/RevenueCat/purchases-android.svg?style=flat)](https://github.com/RevenueCat/purchases-android/releases)

RevenueCat is a powerful, reliable, and free to use in-app purchase server with cross-platform support. Our open-source framework provides a backend and a wrapper around StoreKit and Google Play Billing to make implementing in-app purchases and subscriptions easy. 

Whether you are building a new app or already have millions of customers, you can use RevenueCat to:

  * Fetch products, make purchases, and check subscription status with our [native SDKs](https://docs.revenuecat.com/docs/installation). 
  * Host and [configure products](https://docs.revenuecat.com/docs/entitlements) remotely from our dashboard. 
  * Analyze the most important metrics for your app business [in one place](https://docs.revenuecat.com/docs/charts).
  * See customer transaction histories, chart lifetime value, and [grant promotional subscriptions](https://docs.revenuecat.com/docs/customers).
  * Get notified of real-time events through [webhooks](https://docs.revenuecat.com/docs/webhooks).
  * Send enriched purchase events to analytics and attribution tools with our easy integrations.

Sign up to [get started for free](https://app.revenuecat.com/signup).

## Purchases

*Purchases* is the client for the [RevenueCat](https://www.revenuecat.com/) subscription and purchase tracking system. It is an open source framework that provides a wrapper around `BillingClient` and the RevenueCat backend to make implementing in-app subscriptions in `Android` easy - receipt validation and status tracking included!

## Migrating Guides
| Description | Link |
| --- | --- |
| Migrating from v4.x.x to v5.x.x | [V5 API Migration Guide](./migrations/v5-MIGRATION.md) |
| Migrating from v5.x.x to v6.x.x | [V6 API Migration Guide](./migrations/v6-MIGRATION.md) |



## RevenueCat SDK Features
|   | RevenueCat |
| --- | --- |
✅ | Server-side receipt validation
➡️ | [Webhooks](https://docs.revenuecat.com/docs/webhooks) - enhanced server-to-server communication with events for purchases, renewals, cancellations, and more  
🎯 | Subscription status tracking - know whether a user is subscribed whether they're on iOS, Android or web  
📊 | Analytics - automatic calculation of metrics like conversion, mrr, and churn  
📝 | [Online documentation](https://docs.revenuecat.com/docs) up to date  
🔀 | [Integrations](https://www.revenuecat.com/integrations) - over a dozen integrations to easily send purchase data where you need it  
💯 | Well maintained - [frequent releases](https://github.com/RevenueCat/purchases-android/releases)  
📮 | Great support - [Help Center](https://revenuecat.zendesk.com) 

## Getting Started
For more detailed information, you can view our complete documentation at [docs.revenuecat.com](https://docs.revenuecat.com/docs).

Please follow the [Quickstart Guide](https://docs.revenuecat.com/docs/) for more information on how to install the SDK.

Or view our Android sample app:
- [MagicWeather](examples/MagicWeather)

## Requirements
- Java 8+
- Kotlin 1.5.0+
- Minimum target: Android 4.0+ (API level 14+)

## SDK Reference
Our full SDK reference [can be found here](https://sdk.revenuecat.com/android/index.html).

## Contributing
Contributions are always welcome! To learn how you can contribute, please see the [Contributing Guide](./Contributing/CONTRIBUTING.md).
