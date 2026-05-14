import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension

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

// `configureConditionalPublishing` in the `revenuecat-public-library` convention
// plugin skips applying `com.vanniktech.maven.publish` when
// `ANDROID_VARIANT_TO_PUBLISH` contains `customEntitlementComputation` (this
// module has no such variant). Gate the configuration on the plugin actually
// being applied so the build still works for that publish path.
plugins.withId("com.vanniktech.maven.publish") {
    extensions.configure<MavenPublishBaseExtension> {
        configure(
            AndroidSingleVariantLibrary(
                variant = "defaultsRelease",
                sourcesJar = true,
                publishJavadocJar = true,
            ),
        )
    }
}

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
