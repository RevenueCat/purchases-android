### Breaking Changes
* Support BC7 (#1737) via Toni Rico (@tonidero)

#### New APIs:
- `SubscriptionOption.installmentInfo` which contains information about installment plans you might want to display in your paywall.
- `PurchasesConfiguration.pendingTransactionsForPrepaidPlansEnabled` allows to enable pending prepaid subscriptions.

#### Breaking changes
- Increase min Sdk to 21. This was increased in BC7
- Increase min kotlin version to 1.7.0
- Remove `UpgradeInfo`
- Remove all `purchasePackage` and `purchaseProduct` methods using `UpgradeInfo`. If you want to perform upgrades/downgrades, please migrate to use the `purchase(PurchaseParams)` method, which has options to perform that operation.

For a full migration guide check [v8-MIGRATION.md](migrations/v8-MIGRATION.md)

### Other Changes
* Bump kotlin to 1.8 (#1726)
