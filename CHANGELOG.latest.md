#### Introducing Signature Verification (beta):

This new feature prevents MitM attacks between the SDK and the RevenueCat server.
With verification enabled, the SDK ensures that the response created by the server was not modified by a third-party, and the entitlements received are exactly what was sent.
This is 100% opt-in. `EntitlementInfos` and `EntitlementInfo` have a new `VerificationResult` property, which will indicate the validity of the responses when this feature is enabled.

```kotlin
val purchasesConfiguration = PurchasesConfiguration.Builder(application, apiKey)
	.entitlementVerificationMode(EntitlementVerificationMode.informational)
	.build()
val purchases = Purchases.configure(purchasesConfiguration)

purchases.getCustomerInfoWith(
	onError = {...},
	onSuccess = { customerInfo -> 
		if (customerInfo.entitlements.verification != VerificationResult.SUCCESS) {
			println("Entitlements could not be verified")
		}
	}
)
```

#### Grace period for entitlements

With this new feature, we will give users an up-to 3 day grace period to reconnect after the
cached subscription has expired. After this grace period has passed, users will lose their entitlements until they
reconnect with our backend in order to validate that the subscription is still active.

### New Features
* Entitlements verification (#841) via Toni Rico (@tonidero)
* Grant grace period for entitlements when the backend is offline (#851) via Toni Rico (@tonidero)
### Other Changes
* Log message when trying configure the SDK multiple times (#868) via Toni Rico (@tonidero)
* Fix docs deployment paths (#854) via Toni Rico (@tonidero)
* Update CHANGELOG with improved notes about SDK issue (#862) via Toni Rico (@tonidero)
