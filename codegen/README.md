# Purchases Codegen Plugin

A Gradle plugin that talks to the RevenueCat API v2 at build time, pulls down your entitlements and offerings, and generates type safe Kotlin code so you never have to write `entitlements["premium_access"]` again.

## Before and After

Before: hard coded string keys, no autocomplete, runtime errors may occur.

```kotlin
val isPremium = customerInfo.entitlements["premium_access"]?.isActive == true
val offering = offerings.getOffering("perplexity")
val monthly = offering?.getPackage("$rc_monthly")
```

After: type safe, IDE autocompleted, compile time checked.

```kotlin
val isPremium = customerInfo.isPremiumAccessActive
val offering = offerings.perplexity
val monthly = offering?.perplexityMonthly
```

Key advantages:

- **IDE autocomplete**: dashboard keys surface as typed properties; no need to remember or copy paste them.
- **Compile time safety**: typos become build errors instead of runtime crashes.
- **Always in sync**: the plugin fetches your latest entitlements and offerings at build time, so there is no separate constants file to maintain by hand.

## Setup

The plugin is published to Maven Central as part of the RevenueCat Purchases SDK. The plugin ID is `com.revenuecat.purchases.codegen` and the artifact is `com.revenuecat.purchases:purchases-codegen-plugin`. The plugin version always matches the Purchases SDK version, so use the same version string you already use for the SDK itself.

### 1. Apply the plugin

Add the plugin to your version catalog in `gradle/libs.versions.toml`:

```toml
[versions]
purchases = "PURCHASES_VERSION"

[plugins]
revenuecat-codegen = { id = "com.revenuecat.purchases.codegen", version.ref = "purchases" }
```

Declare it in your root `build.gradle.kts` so Gradle resolves the plugin for all subprojects:

```kotlin
plugins {
    alias(libs.plugins.revenuecat.codegen) apply false
}
```

Then apply it in your app module's `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.revenuecat.codegen)
}
```

### 2. Configure the plugin

Add a `revenuecat { }` block to the same file. Only `apiKey`, `projectId`, and `packageName` are required. All other options have sensible defaults.

```kotlin
import com.revenuecat.purchases.codegen.NamingStyle
import com.revenuecat.purchases.codegen.OfflineMode

revenuecat {
    // Required
    apiKey.set("sk_your_v2_secret_key")       // v2 secret key from Project Settings -> API Keys
    projectId.set("proj_your_project_id")     // found in Project Settings
    packageName.set("com.example.app.rc")     // must be a package you own; see note below

    // Optional depending on your preferences:
    
    // How long (minutes) the fetched schema is cached before re-fetching.
    // Set to 0 to force a fresh fetch on every build.
    // Default: 30
    cacheTtlMinutes.set(30L)

    // What to do when the API fetch fails (network error, missing key, etc.).
    // Default: USE_CACHE_OR_SKIP
    offlineMode.set(OfflineMode.USE_CACHE_OR_SKIP)

    // Naming convention for generated Kotlin identifiers.
    // CAMEL_CASE: premium_access -> premiumAccess  (recommended)
    // SNAKE_CASE: premium_access -> premium_access
    // AS_IS:      premium_access -> premium_access  (no transformation beyond $rc_ prefix stripping)
    // Default: CAMEL_CASE
    namingStyle.set(NamingStyle.CAMEL_CASE)

    // Toggle individual generation targets. Disable any you do not need
    // to keep the generated source surface small.
    generateEntitlements.set(true)             // RCEntitlementId + EntitlementInfos extensions
    generateOfferings.set(true)                // RCOfferingId + Offerings extensions
    generatePackages.set(true)                 // RCXxxPackageId + Offering extensions per offering
    generateCustomerInfoExtensions.set(true)   // isXxxActive shortcuts on CustomerInfo
}
```

**`apiKey`**: Must be a v2 secret key (starts with `sk_`) with read permissions. Create one in the RevenueCat dashboard under **Project Settings -> API Keys**. This key is used only at build time and is never compiled into your app binary. See [API Key Security](#api-key-security) for how to keep it out of version control.

**`packageName`**: Must be a package you own (e.g. `com.myapp.rc`). Avoid `com.revenuecat.*` packages. The RevenueCat SDK ships a ProGuard/R8 consumer rule `-keep class com.revenuecat.** { *; }` that prevents any class under that namespace from being minimized or obfuscated in release builds. If your generated code lands in `com.revenuecat.*`, it will be retained in full even when unused, bloating your APK.

**`cacheTtlMinutes`**: The plugin writes the fetched schema to `build/revenuecat/cache/revenuecat-schema.json`. On subsequent builds it checks the file's timestamp and skips the network call if the file is younger than `cacheTtlMinutes`. Setting this to `0` forces a fresh fetch every build, which is useful in CI when you want generated code to always match the latest dashboard state, but it adds network latency and rate limit risk to every build.

**`offlineMode`**: Controls behavior when the API fetch step fails. `USE_CACHE_OR_SKIP` is safe for local development because it never breaks the build when credentials are missing or the network is unavailable. `FAIL` is the right choice for a release CI pipeline where a silent fallback is not acceptable. See [Offline Mode](#offline-mode) for a pattern that applies each mode conditionally based on environment.

**`namingStyle`**: Controls how dashboard lookup keys become Kotlin identifiers. The `$rc_` prefix is always stripped first, regardless of style. `CAMEL_CASE` is the best fit for most Kotlin codebases. See [Naming Styles](#naming-styles) for a full comparison table.

**`generateEntitlements` / `generateOfferings` / `generatePackages` / `generateCustomerInfoExtensions`**: Each flag controls one category of generated output. Disable any that you do not use to keep the generated source surface small. For example, if you iterate `offering.availablePackages` dynamically rather than accessing packages by name, set `generatePackages.set(false)`.

### 3. Build your project

Run any standard build task. The plugin wires `rcFetchSchema` and `rcGenerateCode` into your compile tasks automatically, so generation runs before compilation without any manual task invocation.

```bash
./gradlew assembleDebug
```

Generated files are written to `build/generated/revenuecat/kotlin/`. After the first successful build, do **File -> Sync Project with Gradle Files** in Android Studio so the IDE picks up the new source directory and provides correct autocomplete.

## What Gets Generated

Given a project with a `premium_access` entitlement and a `perplexity` offering containing `$rc_monthly` and `$rc_annual` packages, the plugin generates the following.

### Entitlement ID constants

The plugin generates a single `RCEntitlementId` object with one constant per entitlement. Using constants instead of raw strings lets the compiler catch typos that would otherwise only fail at runtime.

```kotlin
object RCEntitlementId {
    const val PREMIUM_ACCESS: String = "premium_access"
}
```

### Type safe `EntitlementInfos` extensions

For each entitlement, the plugin generates two extension properties on `EntitlementInfos`: an accessor that returns the `EntitlementInfo` (or `null` if the entitlement is not present in the customer's subscription), and a boolean convenience property for the common `isActive` check.

```kotlin
val EntitlementInfos.premiumAccess: EntitlementInfo?
    get() = this["premium_access"]

val EntitlementInfos.isPremiumAccessActive: Boolean
    get() = this["premium_access"]?.isActive == true
```

### Convenience `CustomerInfo` extensions

The plugin also generates the same active check directly on `CustomerInfo`, so you can skip the `entitlements` lookup entirely when you only need to gate a feature behind a single entitlement.

```kotlin
val CustomerInfo.isPremiumAccessActive: Boolean
    get() = this.entitlements["premium_access"]?.isActive == true
```

### Offering ID constants and `Offerings` extensions

Each offering gets a constant in `RCOfferingId` and a typed accessor on `Offerings`. The accessor wraps `getOffering()` so you get a null safe property call instead of passing a raw string to a function.

```kotlin
object RCOfferingId {
    const val PERPLEXITY: String = "perplexity"
}

val Offerings.perplexity: Offering?
    get() = this.getOffering("perplexity")
```

### Per offering package ID constants and `Offering` extensions

Package extension properties are prefixed with the offering name to prevent collisions when multiple offerings share the same package lookup key (e.g. `$rc_monthly`). Each offering gets its own ID constant object and its own set of extension properties on `Offering`.

```kotlin
object RCPerplexityPackageId {
    const val MONTHLY: String = "$rc_monthly"
    const val ANNUAL: String  = "$rc_annual"
}

val Offering.perplexityMonthly: Package?
    get() = this.getPackage("$rc_monthly")

val Offering.perplexityAnnual: Package?
    get() = this.getPackage("$rc_annual")
```

## Running Tasks Manually

The plugin registers two Gradle tasks that you can invoke directly when you need to fetch or regenerate without running a full build.

```bash
# Fetch schema from the API (skips if cache is still fresh)
./gradlew :app:rcFetchSchema

# Regenerate Kotlin source files from the existing cache
./gradlew :app:rcGenerateCode

# Force a full refresh: wipe the cache, then fetch and regenerate
rm -rf app/build/revenuecat/cache
./gradlew :app:rcFetchSchema :app:rcGenerateCode
```

You can also run these from the Gradle tool window in Android Studio under `app > Tasks > revenuecat`.

## Refreshing After Dashboard Changes

The generated code reflects your RevenueCat dashboard at the time of the last fetch. If you add, rename, or remove entitlements, offerings, or packages in the dashboard, the local cache becomes stale. It expires automatically after `cacheTtlMinutes` (default 30 minutes), so a normal build picks up changes within that window.

To pick up changes immediately without waiting for the TTL to expire:

```bash
rm -rf app/build/revenuecat/cache
./gradlew :app:rcFetchSchema :app:rcGenerateCode
```

After regeneration, do **File -> Sync Project with Gradle Files** in Android Studio so the IDE picks up the updated generated sources and provides correct autocomplete.

To always fetch on every build, set `cacheTtlMinutes` to `0`. Note that this adds network latency to every build and increases the chance of hitting API rate limits on projects with many offerings:

```kotlin
revenuecat {
    cacheTtlMinutes.set(0)
}
```

## Naming Styles

The `namingStyle` option controls how RevenueCat dashboard lookup keys are turned into Kotlin identifiers. Before any style is applied, the `$rc_` prefix is stripped. Kotlin reserved words are automatically escaped with backticks so the generated code always compiles.

| Lookup key | `CAMEL_CASE` (default) | `SNAKE_CASE` | `AS_IS` |
|---|---|---|---|
| `premium_access` | `premiumAccess` | `premium_access` | `premium_access` |
| `ProPhoto` | `proPhoto` | `pro_photo` | `ProPhoto` |
| `$rc_monthly` | `monthly` | `monthly` | `monthly` |
| `$rc_annual` | `annual` | `annual` | `annual` |

`CAMEL_CASE` is the best fit for most Kotlin codebases because it follows standard Kotlin naming conventions. `SNAKE_CASE` is useful if your team prefers that style for generated code or if you are mirroring an existing naming convention. `AS_IS` preserves the exact lookup key after stripping the `$rc_` prefix, which is useful when lookup keys are already well formed identifiers and you want a direct one to one mapping with no transformation.

The naming style affects all generated property names. For example, with `CAMEL_CASE` a `premium_access` entitlement produces `RCEntitlementId.PREMIUM_ACCESS` (the constant), `EntitlementInfos.premiumAccess`, and `CustomerInfo.isPremiumAccessActive`. The constant in the ID object is always `UPPER_SNAKE_CASE` regardless of the naming style, because constants follow a separate convention in Kotlin.

## Offline Mode

The `offlineMode` option controls what happens when the API fetch step fails. A fetch can fail because of a network error, a connection timeout, an incorrect API key, or a missing key in an environment that has not configured credentials.

- **`USE_CACHE_OR_SKIP`** (default): If a stale cache exists on disk, it is used and a warning is logged showing the cache age. If no cache exists at all, code generation is silently skipped and the build continues without any generated sources. This behavior keeps local and CI builds working without intervention when credentials are not available or the network is unreachable.

- **`FAIL`**: The build fails immediately with a `GradleException` describing the error. Use this on release CI pipelines where a hard guarantee is required that the generated code reflects the latest dashboard state and a silent fallback is not acceptable.

A practical pattern is to apply each mode conditionally based on whether the build is running in CI:

```kotlin
import com.revenuecat.purchases.codegen.OfflineMode

val isCI = providers.environmentVariable("CI").isPresent

revenuecat {
    apiKey.set(providers.environmentVariable("REVENUECAT_API_KEY").getOrElse(""))
    projectId.set(providers.environmentVariable("REVENUECAT_PROJECT_ID").getOrElse(""))
    packageName.set("com.example.app.rc")
    offlineMode.set(if (isCI) OfflineMode.FAIL else OfflineMode.USE_CACHE_OR_SKIP)
}
```

## API Key Security

The `apiKey` and `projectId` values are used only at build time by the Gradle plugin. They are never compiled into your app binary and never shipped to users. The generated Kotlin files contain only plain lookup key strings like `"premium_access"`, not your secret key.

That said, you should not commit `sk_` keys directly in `build.gradle.kts`. The recommended approach for local development is to read them from `local.properties`, which you keep out of version control:

```kotlin
// build.gradle.kts
val localProps = java.util.Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

revenuecat {
    apiKey.set(localProps.getProperty("REVENUECAT_API_KEY", ""))
    projectId.set(localProps.getProperty("REVENUECAT_PROJECT_ID", ""))
    packageName.set("com.example.app.rc")
}
```

```properties
# local.properties  (add to .gitignore)
REVENUECAT_API_KEY=sk_your_v2_secret_key
REVENUECAT_PROJECT_ID=proj_your_project_id
```

For CI, inject them as environment variables:

```kotlin
revenuecat {
    apiKey.set(providers.environmentVariable("REVENUECAT_API_KEY").getOrElse(""))
    projectId.set(providers.environmentVariable("REVENUECAT_PROJECT_ID").getOrElse(""))
    packageName.set("com.example.app.rc")
}
```

If the key is missing and `offlineMode` is `USE_CACHE_OR_SKIP` (the default), the plugin falls back to the cached schema or skips generation silently. This means local builds without credentials still work as long as a team member has fetched the schema at least once and the cache file is present.

## Troubleshooting

**"No RevenueCat schema cache found. Skipping code generation."**
The API fetch failed silently because `offlineMode` is `USE_CACHE_OR_SKIP` and no cache file exists yet. Set `offlineMode.set(OfflineMode.FAIL)` temporarily to surface the real error. The cause is usually an incorrect `apiKey`, an incorrect `projectId`, or a network issue preventing the initial fetch.

**"revenuecat.apiKey must be configured in your build script."**
The `apiKey` property is empty or not set. Check that `local.properties` or your environment variable is populated and that `build.gradle.kts` reads it correctly before passing it to `apiKey.set(...)`.

**First build is slow**
If your project has many offerings, the plugin fetches package details for each offering individually and applies a short delay between requests to respect API rate limits. Subsequent builds read from the local cache and are fast.

**Generated code not showing up in the IDE**
After the first generation, do **File -> Sync Project with Gradle Files** in Android Studio to make the IDE aware of the new source directory. If the directory still does not appear, verify that `build/generated/revenuecat/kotlin/` was created and that `rcGenerateCode` completed without errors in the build output.

**`kspDebugKotlin` implicit dependency error**
This happens when the plugin has not registered `rcGenerateCode` as a dependency of KSP or KAPT compile tasks. Ensure you are on the latest plugin version. Older versions only wired `compileDebugKotlin` and missed KSP/KAPT tasks, which causes Gradle's dependency validation to fail on projects that use Hilt, Room, or other annotation processors.

## Trade offs

The plugin is a good fit for **stable identifiers**. Entitlement IDs rarely change once set, and the compile time safety it provides is a clear win there. Offerings and packages can also benefit, particularly when their IDs are stable and you want autocomplete and refactoring support across a large codebase.

For **highly dynamic offerings**, keep one trade off in mind. Any dashboard change, such as adding a new package or renaming an offering, requires a new build and a new app release for the generated code to reflect it. If you access `offering.availablePackages` directly, your existing app can pick up new packages without a release, because the package list is fetched from the RevenueCat backend at runtime.

In practice, adding a new package that you intend to use in code requires writing new code anyway, which means a new build is required regardless. But if you manage your paywall presentation entirely from the dashboard without touching code, the raw API gives you more flexibility.
