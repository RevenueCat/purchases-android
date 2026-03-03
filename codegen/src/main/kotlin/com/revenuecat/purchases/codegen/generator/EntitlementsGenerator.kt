package com.revenuecat.purchases.codegen.generator

import com.revenuecat.purchases.codegen.NamingConfig
import com.revenuecat.purchases.codegen.NamingStyle
import com.revenuecat.purchases.codegen.api.EntitlementSchema
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal class EntitlementsGenerator(
    private val packageName: String,
    private val namingStyle: NamingStyle
) {

    private val entitlementInfos = ClassName("com.revenuecat.purchases", "EntitlementInfos")
    private val entitlementInfo = ClassName("com.revenuecat.purchases", "EntitlementInfo")

    internal fun generate(entitlements: List<EntitlementSchema>, outputDir: File) {
        generateIdObject(entitlements, outputDir)
        generateExtensions(entitlements, outputDir)
    }

    private fun generateIdObject(entitlements: List<EntitlementSchema>, outputDir: File) {
        val objectBuilder = TypeSpec.objectBuilder("RCEntitlementId")
            .addKdoc("Auto-generated entitlement ID constants from RevenueCat dashboard.")

        for (entitlement in entitlements) {
            val constName = NamingConfig.toConstant(entitlement.lookupKey)
            objectBuilder.addProperty(
                PropertySpec.builder(constName, String::class)
                    .addModifiers(KModifier.CONST)
                    .initializer("%S", entitlement.lookupKey)
                    .addKdoc("%L", entitlement.displayName)
                    .build()
            )
        }

        FileSpec.builder(packageName, "RCEntitlementId")
            .addType(objectBuilder.build())
            .build()
            .writeTo(outputDir)
    }

    private fun generateExtensions(entitlements: List<EntitlementSchema>, outputDir: File) {
        val fileBuilder = FileSpec.builder(packageName, "EntitlementInfosExt")
            .addFileComment("Auto-generated extension properties for EntitlementInfos.")

        for (entitlement in entitlements) {
            val propNameRaw = NamingConfig.toUnescapedIdentifier(entitlement.lookupKey, namingStyle)
            val propName = NamingConfig.escapeIfReservedKeyword(propNameRaw)

            fileBuilder.addProperty(
                PropertySpec.builder(propName, entitlementInfo.copy(nullable = true))
                    .receiver(entitlementInfos)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return this[%S]", entitlement.lookupKey)
                            .build()
                    )
                    .addKdoc("%L", entitlement.displayName)
                    .build()
            )

            val activePropName = NamingConfig.escapeIfReservedKeyword(
                "is${propNameRaw.replaceFirstChar { it.uppercase() }}Active"
            )
            fileBuilder.addProperty(
                PropertySpec.builder(activePropName, Boolean::class)
                    .receiver(entitlementInfos)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return this[%S]?.isActive == true", entitlement.lookupKey)
                            .build()
                    )
                    .addKdoc("Whether the %L entitlement is currently active.", entitlement.displayName)
                    .build()
            )
        }

        fileBuilder.build().writeTo(outputDir)
    }
}
