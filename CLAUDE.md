# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Building and Testing
```bash
# Build all modules
./gradlew build

# Run unit tests
./gradlew test

# Run unit tests with backend integration tests
./gradlew test -PRUN_INTEGRATION_TESTS=true

# Run unit tests for specific modules
./gradlew :purchases:testDefaultsDebugUnitTest
./gradlew :ui:revenuecatui:testDefaultsDebugUnitTest

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Run instrumentation tests for specific modules
./gradlew :purchases:connectedDefaultsDebugAndroidTest
./gradlew :ui:revenuecatui:connectedDefaultsDebugAndroidTest

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

# API compatibility check
./scripts/api-check.sh

# Generate API signatures
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
- **`:ui:revenuecatui`** - Jetpack Compose UI module for paywalls and customer center (min SDK 24)
- **`:ui:debugview`** - Debug utilities and UI for development
- **`:feature:amazon`** - Amazon Appstore integration as separate feature module
- **`:bom`** - Bill of Materials for dependency management
- **`:baselineprofile`** - Performance optimization profiles
- **`:integration-tests`** - Integration test suite
- **`:examples/*`** - Sample applications and testers

### Key Architectural Patterns

#### Core Purchases Module
- **Orchestrator Pattern**: `PurchasesOrchestrator` as central coordinator
- **Abstract Factory**: `BillingAbstract` for different store implementations
- **Repository Pattern**: `Backend` for networking, `DeviceCache` for local storage
- **Manager Pattern**: `IdentityManager`, `SubscriberAttributesManager`, `EventsManager`

#### UI Modules
- **MVVM Pattern**: ViewModels with Jetpack Compose UI
- **Main Components**: `PaywallViewModel`, `CustomerCenterViewModel`

### Package Organization
```
com.revenuecat.purchases/
├── common/                  # Shared utilities, networking, caching, events
├── google/                  # Google Play Billing implementation
├── amazon/                  # Amazon Appstore implementation
├── identity/                # User identity management
├── subscriberattributes/    # Subscriber attributes system
├── paywalls/               # Paywall data and components
├── customercenter/         # Customer center functionality
├── models/                 # Data models and DTOs
├── interfaces/             # Public API contracts
└── utils/                  # Utility functions
```

### Product Flavors
The `purchases` module has 2 flavors:
- **`defaults`** - Standard Google Play implementation
- **`customEntitlementComputation`** - Custom entitlement computation variant

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
- **Detekt**: Static code analysis with auto-correction
- **Pre-commit Hook**: Runs detekt before each commit
- **API Compatibility**: Metalava for API checking and signature generation
- **Baseline Profiles**: Performance optimization for improved startup

### Main Entry Points
- **`Purchases`** class: Primary SDK entry point
- **`PurchasesConfiguration`**: Configuration builder
- **UI Components**: `Paywall()`, `CustomerCenter()` composables

### Key Dependencies
- **Kotlin**: Primary language with coroutines for async operations
- **Jetpack Compose**: Modern UI framework
- **Kotlinx Serialization**: JSON handling
- **Google Tink**: Cryptography
- **OkHttp**: HTTP networking foundation

## Build Configuration

### Gradle Structure
- **Multi-module**: Uses `include()` statements in `settings.gradle.kts`
- **Version Catalogs**: Centralized dependency management in `gradle/libs.versions.toml`
- **Flavors**: Support for different store implementations
- **Build Types**: Debug and release configurations

### Target Specifications
- **Compile SDK**: 34
- **Min SDK**: 21 (24 for UI modules)
- **Java**: 8+
- **Kotlin**: 1.8.0+

## Development Notes

### Backend Integration Testing
- Enable with `-PRUN_INTEGRATION_TESTS=true`
- Uses real API keys and tokens from environment variables
- Located in `backend_integration_tests` packages

### Sample Applications
- **MagicWeather**: Standard sample app
- **MagicWeatherCompose**: Compose-based sample
- **purchase-tester**: Testing app for purchase flows
- **paywall-tester**: Testing app for paywall UI

### Release Process
- **Fastlane**: Automated release management
- **Version Management**: Centralized in `.version` file
- **Publishing**: Maven Central through Sonatype
- **Documentation**: Dokka for API docs