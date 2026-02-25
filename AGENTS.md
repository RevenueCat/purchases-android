# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Common Development Commands

### Building and Testing
```bash
# Build all modules
./gradlew build

# Run unit tests
./gradlew test

# Run unit tests with backend integration tests
./gradlew test -PRUN_INTEGRATION_TESTS=true

# Run unit tests for specific modules (flavor format: {apis}{billingclient}{buildType})
./gradlew :purchases:testDefaultsBc8DebugUnitTest
./gradlew :purchases:testDefaultsBc7DebugUnitTest
./gradlew :ui:revenuecatui:testDefaultsBc8DebugUnitTest
./gradlew :ui:revenuecatui:testDefaultsBc7DebugUnitTest

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Run instrumentation tests for specific modules
./gradlew :purchases:connectedDefaultsBc8DebugAndroidTest
./gradlew :purchases:connectedDefaultsBc7DebugAndroidTest
./gradlew :ui:revenuecatui:connectedDefaultsBc8DebugAndroidTest
./gradlew :ui:revenuecatui:connectedDefaultsBc7DebugAndroidTest

# Run integration tests (requires device/emulator)
./gradlew :integration-tests:connectedDebugAndroidTest
```

### Code Quality and Analysis
```bash
# Run lint (static code analysis)
./gradlew lint

# Run detekt (static code analysis)
./gradlew detektAll

# Run detekt with auto-correct
./gradlew detektAll --auto-correct

# Create detekt baseline
./gradlew detektAllBaseline

# API compatibility check (using Metalava)
./scripts/api-check.sh

# Generate API signatures (using Metalava)
./scripts/api-dump.sh
```

### UI Screenshot Testing (Paparazzi)
```bash
# Generate/verify snapshots
./gradlew :ui:revenuecatui:recordPaparazziDebug
./gradlew :ui:revenuecatui:verifyPaparazziDebug
```

### Fastlane Commands
```bash
# Setup development environment (links pre-commit hooks)
bundle exec fastlane setup_dev

# Run tests
bundle exec fastlane test

# Run backend integration tests
bundle exec fastlane run_backend_integration_tests
```

## Project Architecture

### Module Structure
This is a multi-module Android project with clear separation of concerns:

- **`:purchases`** - Core SDK module containing main API, business logic, networking, billing abstractions
- **`:ui:revenuecatui`** - Jetpack Compose UI module for paywalls and customer center (min SDK 24, depends on `:purchases`)
- **`:ui:debugview`** - Debug utilities and UI for development (depends on `:purchases`)
- **`:feature:amazon`** - Amazon Appstore integration as separate feature module (depends on `:purchases`)
- **`:bom`** - Bill of Materials for dependency management
- **`:baselineprofile`** - Performance optimization profiles
- **`:integration-tests`** - Integration test suite
- **`:examples/*`** - Sample applications and testers

### Key Architectural Patterns

#### Core Purchases Module
- **Orchestrator Pattern**: `PurchasesOrchestrator` as central coordinator
- **Abstract Factory**: `BillingAbstract` for different store implementations
- **Backend/Cache Layer**: `Backend` for networking, `DeviceCache` for local storage
- **Manager Pattern**: `IdentityManager`, `SubscriberAttributesManager`, `EventsManager`

#### UI Modules
- **MVVM Pattern**: ViewModels with Jetpack Compose UI
- **Main Components**: `PaywallViewModel`, `CustomerCenterViewModel`

### Product Flavors
The `purchases` module has 2 flavor dimensions:
- **`apis`**: `defaults` (standard) or `customEntitlementComputation` (custom entitlement computation variant)
- **`billingclient`**: `bc8` (default, Billing Client 8) or `bc7` (Billing Client 7)

Variant names combine both dimensions, e.g. `defaultsBc8Debug`, `customEntitlementComputationBc7Release`.

### API Annotations
- **`@InternalRevenueCatAPI`** - APIs that are public only to be accessible by other modules or hybrid SDKs, not intended for external developer use
- **`@ExperimentalPreviewRevenueCatPurchasesAPI`** - Public APIs for developers that may change before being made stable
- **`@ExperimentalPreviewRevenueCatUIPurchasesAPI`** - Same as above but for the `:ui:revenuecatui` module

## Testing Framework

### Technologies Used
- **JUnit 4** - Primary testing framework
- **Mockk** - Mocking framework for Kotlin
- **Robolectric** - Android unit testing
- **Espresso** - UI testing
- **AssertJ** - Fluent assertions
- **Paparazzi** - Compose UI screenshot testing
- **Coroutines Test** - Async testing

### Test Structure
- **Unit Tests**: `src/test/` directories
- **Instrumentation Tests**: `src/androidTest/` directories
- **Integration Tests**: `/integration-tests/` module
- **Backend Integration Tests**: `src/test/.../backend_integration_tests/` (enabled with `-PRUN_INTEGRATION_TESTS=true`)

## Development Workflow

### Environment Setup
1. Install sdkman and run `sdk env install` in project root
2. Run `bundle exec fastlane setup_dev` to link pre-commit hooks
3. Pre-commit hooks automatically run detekt on commits

### Code Quality
- **Lint**: Android lint for static code analysis (`./gradlew lint`)
- **Detekt**: Static code analysis with auto-correction (`./gradlew detektAll`)
- **Pre-commit Hook**: Runs detekt before each commit
- **API Compatibility**: Metalava for API checking and signature generation
- **Baseline Profiles**: Performance optimization for improved startup

### Main Entry Points
- **`Purchases`** class: Primary SDK entry point
- **`PurchasesConfiguration`**: Configuration builder
- **UI Components**: `Paywall()`, `PaywallActivityLauncher`, `CustomerCenter()`, `ShowCustomerCenter` composables

### Key Dependencies
- **Kotlin**: Primary language with coroutines for async operations
- **Jetpack Compose**: Modern UI framework
- **Kotlinx Serialization**: JSON handling
- **Google Tink**: Cryptography
- **java.net.HttpURLConnection**: HTTP networking foundation

## Build Configuration

### Gradle Structure
- **Multi-module**: Uses `include()` statements in `settings.gradle.kts`
- **Version Catalogs**: Centralized dependency management in `gradle/libs.versions.toml`
- **Flavors**: Support for different API variants and billing client versions
- **Build Types**: Debug and release configurations

### Target Specifications
- **Compile SDK**: 35
- **Min SDK**: 21 (24 for UI modules)
- **Java**: 8+
- **Kotlin**: 2.0.21 (language level 1.8)

## Development Notes

### Backend Integration Testing
- Enable with `-PRUN_INTEGRATION_TESTS=true`
- Uses real API keys and tokens from environment variables
- Located in `backend_integration_tests` packages

### Sample Applications
- **MagicWeather**: Standard sample app
- **MagicWeatherCompose**: Compose-based sample
- **CustomEntitlementComputationSample**: Sample for custom entitlement computation flavor
- **purchase-tester**: Testing app for purchase flows
- **paywall-tester**: Testing app for paywall UI
- **web-purchase-redemption-sample**: Sample for web purchase redemption

### Test Apps
- **e2etests**: End-to-end tests
- **sdksizetesting**: SDK size measurement
- **testpurchasesandroidcompatibility**: Android compatibility testing for purchases
- **testpurchasesuiandroidcompatibility**: Android compatibility testing for UI

### Release Process
- **Fastlane**: Automated release management
- **Version Management**: Centralized in `.version` file
- **Publishing**: Maven Central through Sonatype
- **Documentation**: Dokka for API docs

### Pull Request Labels

When creating a pull request, **always add one of these labels** to categorize the change. These labels determine automatic version bumps and changelog generation:

| Label      | When to Use                                                                         |
|------------|-------------------------------------------------------------------------------------|
| `pr:feat`  | New user-facing features or enhancements                                            |
| `pr:fix`   | Bug fixes                                                                           |
| `pr:other` | Internal changes, refactors, CI, docs, or anything that shouldn't trigger a release |

**Additional scope labels** (add alongside the primary label above):
- `pr:RevenueCatUI` — Changes specific to the RevenueCatUI module (paywalls, customer center)
- `feat:Paywalls_V2` — Changes related to Paywalls V2 (requires `pr:RevenueCatUI` as well)
- `feat:Customer Center` — Changes related to Customer Center (requires `pr:RevenueCatUI` as well)

## Guardrails

- **Never commit Claude-related files** — do not stage or commit `.claude/` directory, `settings.local.json`, or any AI tool configuration files
- **Never commit API keys or secrets** — do not stage or commit API keys, tokens, credentials, or any sensitive data
