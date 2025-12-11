package com.revenuecat.purchases.android.buildlogic.ktx

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal val Project.versionCatalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal val Project.libs: VersionCatalog
    get() = versionCatalog
