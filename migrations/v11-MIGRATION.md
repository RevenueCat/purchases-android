# V11 API Migration Guide

This release updates the SDK's build toolchain to Android Gradle Plugin 9 and Kotlin 2.2.21. Our new minimum version is Kotlin 2.1.0+.


### New minimum Kotlin version

RevenueCat SDK v11 bumps minimum Kotlin version to 2.1.0, up from 1.8.0 in v10.

A Kotlin compiler can only read metadata from at most one minor version ahead of itself, so a compiler older than 2.1 cannot read the 2.2.x standard library and the build fails with:

```
Module was compiled with an incompatible version of Kotlin.
The binary version of its metadata is 2.2.0, expected version is 2.0.0.
```

If you see this, update the Kotlin Gradle Plugin in your app to 2.1.0 or newer.

### Reporting undocumented issues:

Feel free to file an issue! [New RevenueCat Issue](https://github.com/RevenueCat/purchases-android/issues/new/).
