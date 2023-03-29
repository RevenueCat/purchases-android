**RevenueCat Android SDK v6** is here!! 😻 

This latest release updates the SDK to use BillingClient 5. This version of BillingClient brings an entire new subscription model which has resulted large changes across the entire SDK.

## Migration Guides
- See the full list of API changes in [our v6 migration doc](https://github.com/RevenueCat/purchases-android/blob/v6/migrations/v6-MIGRATION.md)

- See [Android Native - 5.x to 6.x Migration](https://www.revenuecat.com/docs/android-native-5x-to-6x-migration) for a 
more thorough explanation of the new Google subscription model announced with BillingClient 5 and how to take advantage of them in V6.

**Note:** This release is based off of `6.0.0-rc.1`. Developers migrating from that version shouldn't see any changes. 

## API changes:
There have been a lot of changes since v5! 

Here are the highlights:

### Purchasing API changes

Purchases are now configured using `PurchaseParams.Builder()`. The builder is constructed with either a `Package`,
`Product`, or `SubscriptionOption` and an `Activity`. The builder then has methods for setting the product change
parameters (`oldProductId` and `googleProrationMode`).

To initiate a purchase, simply pass the built `PurchaseParams` and your `PurchaseCallback` to the `purchase()` method.

### API Changes

#### Applying offers on a purchase

In V5, a purchase of a `Package` or `StoreProduct` represented a single purchaseable entity, and free trials or intro
offers would automatically be applied if the user was eligible.

### New `SubscriptionOption` concept

Now, in v6, a `Package` or `StoreProduct` could contain multiple offers along within its base plan. 
When passing a `Package` or `StoreProduct` to `purchase()`, the SDK will use the following logic to choose which 
[SubscriptionOption] to purchase:
*   - Filters out offers with "rc-ignore-offer" tag
*   - Uses [SubscriptionOption] with the longest free trial or cheapest first phase
*   - Falls back to base plan

For more control, create your `PurchaseParams.Builder` with the desired `SubscriptionOption`.
