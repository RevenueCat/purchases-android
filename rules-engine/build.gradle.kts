import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.revenuecat.public.library)
}

metalava {
    filename.set("api.txt")
}

android {
    namespace = "com.revenuecat.purchases.rules"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Other library modules ship with a `billingclient` flavor (`bc8` / `bc7`) and
// publish `defaultsBc8Release` by default via the root `ANDROID_VARIANT_TO_PUBLISH`
// property. This module has no Billing Client dependency, so it stays single-flavor
// and publishes `defaultsRelease` directly. If we ever need a billing-client-aware
// variant here, switch to the standard pattern instead of overriding.
mavenPublishing {
    configure(
        AndroidSingleVariantLibrary(
            variant = "defaultsRelease",
            sourcesJar = true,
            publishJavadocJar = true,
        ),
    )
}

dependencies {
    testImplementation(libs.bundles.test)
}
