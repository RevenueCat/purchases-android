### Migration Guide
- A number of renames, removal of some deprecated functions, and a change to configuration. Amazon Store support is also included Checkout the full list of changes in [migrations/v5-MIGRATION.md]

### API changes:
- See our [RevenueCat V5 API update doc]([migrations/v5-MIGRATION.md]) for API updates.

### Amazon Store Support
We have introduced support for using the Amazon Store. We have extensively tested this, and there are some apps using our pre-release Amazon versions in production.

However, we have found some inconsistencies in the way Amazon Store prices are reported. We are actively working on patching these inconsistencies.

Please help us help you by reporting any issues you find. [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).

You can enable Amazon Store support by configuring the SDK using the new configure function:

```
Purchases.configure(AmazonConfiguration.Builder(this, "public_amazon_sdk_key").build())
```

For more information around configuration please take a look at the [Amazon Store section in our docs](https://docs.revenuecat.com/docs/amazon-platform-resources). The official [Amazon In-App Purchasing docs](https://developer.amazon.com/docs/in-app-purchasing/iap-overview.html) also contain very valuable information, specially around testing and best practices.

If you're updating from an alpha version with Amazon Store support. Remove the in-app-purchasing-2.0.76.jar dependency from your `app/build.gradle` and from the libs folder. Manually adding the jar is no longer required 😄

```
dependencies {
    implementation files('libs/in-app-purchasing-2.0.76.jar') -> Remove this
```

**⚠️ ⚠️  Important ⚠️ ⚠️** Observer mode for Amazon is not supported yet.
 
### Changelog

**Note:** This release is based off of 5.0.0-rc3. These are the changes compared to 5.0.0-rc3:

- Removed deprecated `addAttributionData` functions and `AttributionNetwork` enum
    https://github.com/RevenueCat/purchases-android/pull/506
- Added retries with exponential backoff when reconnecting to Google's billing service
    https://github.com/RevenueCat/purchases-android/pull/489
- Updated to BillingClient 4.1
    https://github.com/RevenueCat/purchases-android/pull/500
- Prevent Amazon from swallowing exceptions
    https://github.com/RevenueCat/purchases-android/pull/499
- Improvements on Amazon implementation preventing a lot of issues related to price and currency parsing
    https://github.com/RevenueCat/purchases-android/pull/503

[Full Changelog](https://github.com/revenuecat/purchases-android/compare/main...5.0.0)
