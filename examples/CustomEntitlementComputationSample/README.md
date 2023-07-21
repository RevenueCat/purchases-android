# Custom Entitlements Computation Example App

This app is useful for testing RevenueCat under Custom Entitlements Computation mode and understanding how it works.

## What is Custom Entitlements Computation mode? 

This is a special behavior mode for RevenueCat SDK, is intended for apps that will do their own entitlement computation separate from RevenueCat. 

Apps using this mode rely on webhooks to signal their backends to refresh entitlements with RevenueCat.

In this mode, RevenueCat will not generate anonymous user IDs, it will not refresh customerInfo cache automatically only when a purchase goes through 
and it will disallow all methods other than those for configuration, switching users, getting offerings and making purchases.

When in this mode, the app should use switchUser() to switch to a different App User ID if needed. 
The SDK should only be configured once the initial appUserID is known.

## Using the app

To use the app, you should do the following: 
- Configure your app in the [RevenueCat dashboard](https://app.revenuecat.com/). No special configuration is needed, but you should contact RevenueCat support
before enabling this mode to ensure that it's the right one for your app. It's highly recommended to set Transfer Behavior to "Keep with original App User ID" in the RevenueCat Dashboard. 
- Update the API key in Constants.kt. You can update the default `appUserID` there too, although apps in this mode should 
always be calling configure only when the appUserID is already known. 
- Update the applicationId in defaultConfig of the app-level build.gradle to match your RevenueCat app configuration.
- Have at least one Offering with at least one Package configured for Android, since this is the one that the purchase button will use. 

Once configured correctly, the app will allow you to log in with different users, and will show a list of all the times CustomerInfoListener fired, as well as 
the values for each one. 

Happy testing!

![sample screenshot](./Sample%20screenshot.png)

## Using Custom Entitlements mode

### Installation: 

This package is available on Maven and can be included via Gradle.

To use this mode, ensure that you install the customEntitlementsComputation build flavor by specifying the dependency as:
```gradle
revenuecat = { module = "com.revenuecat.purchases:purchases@customEntitlementComputation", version.ref = "purchases" }
```
for Gradle version catalogs, or
```gradle
implementation 'com.revenuecat.purchases:purchases@customEntitlementsComputation:6.8.0'
```
for Gradle version properties.

### Configuration: 

The SDK should be configured once the user has already logged in. To configure, call:

```kotlin
val builder = PurchasesConfiguration.Builder(
    this,
    Constants.GOOGLE_API_KEY,
    Constants.defaultAppUserID
)

Purchases.configure(builder.build())
```

### Getting Offerings: 

Call getOfferings through either the Async / Await or completion blocks alternatives:

```kotlin

val offerings = Purchases.sharedInstance.awaitOfferings()

```

### Switching users: 

To switch to a different user, call:

```kotlin
Purchases.sharedInstance.switchUser(newUserID)
```

This will ensure that all purchases made from this point on are posted for the new appUserID. 
After calling this method, you might need to call your backend to refresh entitlements for the new appUserID if they haven't been refreshed already.

### Making purchases:

Call `awaitPurchase()`:

```kotlin
try {
    val (transaction, customerInfo) =
        Purchases.sharedInstance.awaitPurchase(purchaseParams)
    // refresh entitlements with your backend
} catch (error: PurchasesTransactionException) {
    if (error.userCancelled) {
        _uiState.update { it.copy(displayErrorMessage = "User cancelled") }
    } else {
        _uiState.update { it.copy(displayErrorMessage = error.message) }
    }
}
```

### Observing changes to purchases:

To ensure that your app reacts to changes to subscriptions in real time, you can use `customerInfoStream`. This stream will only fire when new `customerInfo` is registered
in RevenueCat, like when a subscription is renewed. If there are no changes from the last value, it will not fire. This means it's not guaranteed to fire on every app open.

```kotlin
Purchases.sharedInstance.updatedCustomerInfoListener =
    UpdatedCustomerInfoListener { customerInfo ->
        updateCustomerInfoInformation(customerInfo)
    }
```
