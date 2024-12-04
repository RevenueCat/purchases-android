package com.revenuecat.dokka.plugin.hideinternal

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DObject
import kotlin.test.Test
import kotlin.test.assertEquals

class HideInternalRevenueCatAPIPluginTest: BaseAbstractTest() {

    @Test
    fun `Should hide annotated functions`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }
        val hideInternalPlugin = HideInternalRevenueCatAPIPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package com.revenuecat.purchases
            |
            |annotation class InternalRevenueCatAPI
            |
            |fun shouldBeVisible() {}
            |
            |@InternalRevenueCatAPI
            |fun shouldBeExcludedFromDocumentation() {}
        """.trimMargin(),
            configuration = configuration,
            pluginOverrides = listOf(hideInternalPlugin)
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val testModule = modules.single { it.name == "root" }
                val testPackage = testModule.packages.single { it.name == "com.revenuecat.purchases" }

                val packageFunctions = testPackage.functions
                assertEquals(expected = 1, actual = packageFunctions.size)
                assertEquals(expected = "shouldBeVisible", actual = packageFunctions[0].name)
            }
        }
    }

    @Test
    fun `Should hide annotated classes`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }
        val hideInternalPlugin = HideInternalRevenueCatAPIPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package com.revenuecat.purchases
            |
            |annotation class InternalRevenueCatAPI
            |
            |class ShouldBeVisible
            |
            |@InternalRevenueCatAPI
            |class ShouldBeExcludedFromDocumentation
        """.trimMargin(),
            configuration = configuration,
            pluginOverrides = listOf(hideInternalPlugin)
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val testModule = modules.single { it.name == "root" }
                val testPackage = testModule.packages.single { it.name == "com.revenuecat.purchases" }

                val packageClasses = testPackage.classlikes.filterIsInstance<DClass>()
                assertEquals(expected = 1, actual = packageClasses.size)
                assertEquals(expected = "ShouldBeVisible", actual = packageClasses[0].name)
            }
        }
    }

    @Test
    fun `Should hide annotated objects`() {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/kotlin/basic/Test.kt")
                }
            }
        }
        val hideInternalPlugin = HideInternalRevenueCatAPIPlugin()

        testInline(
            """
            |/src/main/kotlin/basic/Test.kt
            |package com.revenuecat.purchases
            |
            |annotation class InternalRevenueCatAPI
            |
            |object ShouldBeVisible
            |
            |@InternalRevenueCatAPI
            |object ShouldBeExcludedFromDocumentation
        """.trimMargin(),
            configuration = configuration,
            pluginOverrides = listOf(hideInternalPlugin)
        ) {
            preMergeDocumentablesTransformationStage = { modules ->
                val testModule = modules.single { it.name == "root" }
                val testPackage = testModule.packages.single { it.name == "com.revenuecat.purchases" }

                val packageObjects = testPackage.classlikes.filterIsInstance<DObject>()
                assertEquals(expected = 1, actual = packageObjects.size)
                assertEquals(expected = "ShouldBeVisible", actual = packageObjects[0].name)
            }
        }
    }

}
