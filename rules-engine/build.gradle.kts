plugins {
    alias(libs.plugins.revenuecat.public.library)
}

metalava {
    filename.set("api.txt")
    // Hide everything tagged with this module's internal-API marker (in addition
    // to `com.revenuecat.purchases.InternalRevenueCatAPI`, which the
    // `revenuecat-public-library` convention plugin already hides).
    hiddenAnnotations.add("com.revenuecat.purchases.rules.InternalRulesEngineAPI")
}

android {
    namespace = "com.revenuecat.purchases.rules"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Maven publishing is intentionally not configured for this module yet — see
// the `:rules-engine` short-circuit in `ConfigureConditionalPublishing` in the
// `revenuecat-public-library` convention plugin. The publish wiring (and the
// `purchases-rules-engine` Maven Central artifact) will be enabled once a
// consumer of the rules engine ships, to avoid publishing an empty artifact.

// All public symbols in `:rules-engine` are gated by `@InternalRulesEngineAPI`,
// so the module has nothing to document for SDK consumers. Suppress it from the
// multi-module dokka site (`dokkaHtmlMultiModule`) instead of generating an
// empty page under `docs/{version}/rules-engine/`.
tasks.dokkaHtmlPartial.configure {
    dokkaSourceSets.configureEach {
        suppress.set(true)
    }
}

dependencies {
    testImplementation(libs.bundles.test)
}
