### Identity V3:

#### New methods
- Introduces `logIn`, a new way of identifying users, which also returns whether a new user has been registered in the system. 
`logIn` uses a new backend endpoint. 
- Introduces `logOut`, a replacement for `reset`. 

#### Deprecations / removals
- removes `createAlias`
- deprecates `identify` in favor of `logIn`
- deprecates `reset` in favor of `logOut`
- deprecates `allowSharingPlayStoreAccount` in favor of dashboard-side configuration

    https://github.com/RevenueCat/purchases-android/pull/250
    https://github.com/RevenueCat/purchases-android/pull/260
    https://github.com/RevenueCat/purchases-android/pull/252


### Other changes: 
- Fixed CI issues with creating pull requests
    https://github.com/RevenueCat/purchases-android/pull/324
