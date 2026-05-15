plugins {
    alias(libs.plugins.revenuecat.public.library)
}

metalava {
    // No `api.txt` baseline is committed for this module — the explicit
    // "no public APIs without @InternalRulesEngineAPI" check lives in PR #3480
    // on top of this skeleton. `hiddenAnnotations` is wired up here so a local
    // `metalavaGenerateSignature*` produces a clean dump for spot-checks, and
    // the dump lands under `build/` (gitignored) instead of at the module root.
    filename.set(layout.buildDirectory.file("api-dump.txt").get().asFile.path)
    hiddenAnnotations.add("com.revenuecat.purchases.rules.InternalRulesEngineAPI")
    enforceCheck.set(false)
}

android {
    namespace = "com.revenuecat.purchases.rules"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    testImplementation(libs.bundles.test)
}
