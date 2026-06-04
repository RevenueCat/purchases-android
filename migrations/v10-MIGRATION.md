# V10 API Migration Guide

This release updates the SDK to use Google Play Billing Library 8.3. This version of the Billing Library increased min SDK version to Android 6 (API 23). You can check the full list of changes here: https://developer.android.com/google/play/billing/release-notes#8-3-0

Additionally, we've also increased our target SDK version to 36, and reverted changes done during the v9 release to allow restoring consumed one time products. This option is not available anymore. Please check our docs [here](https://www.revenuecat.com/docs/known-store-issues/play-billing-library/restore-consumable-purchases-bc8)

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
