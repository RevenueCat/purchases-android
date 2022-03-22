### Migration Guide
- A number of renames, removal of some deprecated functions, and a change to configuration. Amazon Store support is also included Checkout the full list of changes in [migrations/v5-MIGRATION.md]

### API changes:
- See our [RevenueCat V5 API update doc]([migrations/v5-MIGRATION.md]) for API updates.

### Amazon Store Support
Introduced support for using Amazon Store. We have extensively tested the Amazon Store, and there are some application already in production using it, but being 5.0.0 the first official release that supports it, there could still be some issues, please report any issue you find. [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).

You can enable Amazon Store support by configuring the SDK using the new configure function:

```
Purchases.configure(AmazonConfiguration.Builder(this, "public_amazon_sdk_key").build())
```

For more information please take a look at the [Amazon Store section in our docs](https://docs.revenuecat.com/docs/amazon-platform-resources). 

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
