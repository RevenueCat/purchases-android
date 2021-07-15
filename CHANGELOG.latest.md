### Identity V3:

In this version, we’ve redesigned the way that user identification works. 
Detailed docs about the new system are available [here](https://docs.revenuecat.com/v3.2/docs/user-ids).

#### New methods
- Introduces `logIn`, a new way of identifying users, which also returns whether a new user has been registered in the system. 
`logIn` uses a new backend endpoint. 
- Introduces `logOut`, a replacement for `reset`. 

#### Deprecations / removals
- deprecates `createAlias` in favor of `logIn`
- deprecates `identify` in favor of `logIn`
- deprecates `reset` in favor of `logOut`
- deprecates `allowSharingPlayStoreAccount` in favor of dashboard-side configuration

    https://github.com/RevenueCat/purchases-android/pull/250
    https://github.com/RevenueCat/purchases-android/pull/260
    https://github.com/RevenueCat/purchases-android/pull/252


### Other changes: 
- Fixed CI issues with creating pull requests
    https://github.com/RevenueCat/purchases-android/pull/324
- Re-enable eTags support to minimize unnecessary network traffic
    https://github.com/RevenueCat/purchases-android/pull/337
- When making a multi-line subscription purchase, all product ids are now sent to the backend.
    https://github.com/RevenueCat/purchases-android/pull/335
- Added `@Throws` annotation to `getPackage`, which could throw `NoSuchElementException`, but it wasn't documented.
    https://github.com/RevenueCat/purchases-android/pull/333
