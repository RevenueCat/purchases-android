# Custom Entitlements Computation Example App

This app is useful for testing RevenueCat under Custom Entitlements Computation mode and understanding how it works.

## What is Custom Entitlements Computation mode? 

This is a special behavior mode for RevenueCat SDK. It's intended for apps that will do their own entitlement computation separate from RevenueCat. 

Apps using this mode rely on webhooks to signal their backends to refresh entitlements with RevenueCat.

In this mode, RevenueCat will not generate anonymous user IDs, or refresh customerInfo cache automatically. CustomerInfo is only refreshed when a purchase goes through 
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

To use this mode, ensure that you install the purchases-custom-entitlement-computation artifact by specifying the dependency as:

```gradle
implementation 'com.revenuecat.purchases:purchases-custom-entitlement-computation:6.8.0'
```

### Configuration: 

The SDK should be configured once the user has already logged in. To configure, call:

```kotlin
Purchases.configureInCustomEntitlementsComputationMode(
    applicationContext,
    Constants.GOOGLE_API_KEY,
    Constants.defaultAppUserID
)
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
val purchaseParams = PurchaseParams.Builder(activity, aPackage).build()

try {
    val (transaction, customerInfo) =
        Purchases.sharedInstance.awaitPurchase(purchaseParams)
    // refresh entitlements with your backend
} catch (error: PurchasesTransactionException) {
    if (error.userCancelled) {
        _uiState.update { it.copy(displayErrorMessage = "Purchase was cancelled by the user") }
    } else {
        val errorMessage = when (error.code) {
            PurchasesErrorCode.ReceiptAlreadyInUseError ->
                "The receipt is already in use by another subscriber. " +
                        "Log in with the previous account or contact support " +
                        "to get your purchases transferred to regain access"
            PurchasesErrorCode.PaymentPendingError ->
                "The purchase is pending and may be completed at a later time. " +
                        "This can happen when awaiting parental approval or going " +
                        "through extra authentication flows for credit cards " +
                        "in some countries"
            PurchasesErrorCode.ProductAlreadyPurchasedError ->
                "Subscription is already purchased. Log in with the account " +
                        "that originally performed this purchase if you're using a different one."
            PurchasesErrorCode.PurchaseNotAllowedError ->
                "Purchasing wasn't allowed, which is common if the card is declined " +
                        "or the purchase is not available in the country " +
                        "you're trying to purchase from."
            PurchasesErrorCode.StoreProblemError ->
                "There was a problem with the Google Play Store. This is a generic " +
                        "Google error, and there's not enough information to " +
                        "determine the cause."
            else -> "FAILED TO PURCHASE: ${error.message}"
        }
        _uiState.update { it.copy(displayErrorMessage = errorMessage) }

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
