### Bugfixes
* Handle other diagnostics-related exceptions (#1076) via Toni Rico (@tonidero)
* Return error in queryPurchases if error connecting to billing client (#1072) via Toni Rico (@tonidero)
### Other Changes
* Trusted entitlements: Add logs with verification mode (#1067) via Toni Rico (@tonidero)
* Sync pending purchases before getting customer info (#1073) via Toni Rico (@tonidero)
* Refactor syncing pending transactions logic out of `Purchases` (#1058) via Toni Rico (@tonidero)
* Refactor CustomerInfo listener and cache logic into CustomerInfoUpdater (#1052) via Toni Rico (@tonidero)
* Trusted entitlements: Add integration tests (#1071) via Toni Rico (@tonidero)
* Trusted entitlements: Add internal mechanism to force signing errors for tests (#1070) via Toni Rico (@tonidero)
