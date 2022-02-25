For more detailed information about RevenueCat Amazon Store support head over to [our docs](https://docs.revenuecat.com/v3.3/docs/android)

## Instructions

Add a new dependency to the build.gradle apart from the regular `purchases` dependency. This new dependency has the classes needed to use Amazon IAP:

```groovy
dependencies {
    implementation 'com.revenuecat.purchases:purchases:5.0.0-rc3'
    implementation 'com.revenuecat.purchases:purchases-store-amazon:5.0.0-rc3'
}
```

Configure the SDK using your **RevenueCat API key specific for Amazon**:

```kotlin
Purchases.configure(AmazonConfiguration.Builder(this, "amzn_api_key").build())
```

## Migration from previous alpha versions of purchases-android with Amazon Store support

Remove the `in-app-purchasing-2.0.76.jar` dependency from your `app/build.gradle`. Manually adding the jar is no longer required :smile:

```
dependencies {
    implementation files('libs/in-app-purchasing-2.0.76.jar') -> Remove this
```

Remove the `in-app-purchasing-2.0.76.jar` from the `libs` folder.

Update to the latest version following the Instructions section.

## Current Limitations

Due to some limitations, RevenueCat will only validate purchases made in production or in Live App Testing and won't validate purchases made with the Amazon App Tester.
