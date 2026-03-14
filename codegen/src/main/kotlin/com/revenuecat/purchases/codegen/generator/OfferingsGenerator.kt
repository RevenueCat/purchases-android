package com.revenuecat.purchases.codegen.generator

import com.revenuecat.purchases.codegen.NamingConfig
import com.revenuecat.purchases.codegen.NamingStyle
import com.revenuecat.purchases.codegen.api.OfferingSchema
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

internal class OfferingsGenerator(
    private val packageName: String,
    private val namingStyle: NamingStyle,
) {

    private val offerings = ClassName("com.revenuecat.purchases", "Offerings")
    private val offering = ClassName("com.revenuecat.purchases", "Offering")

    internal fun generate(offeringsList: List<OfferingSchema>, outputDir: File) {
        generateIdObject(offeringsList, outputDir)
        generateExtensions(offeringsList, outputDir)
    }

    private fun generateIdObject(offeringsList: List<OfferingSchema>, outputDir: File) {
        val objectBuilder = TypeSpec.objectBuilder("RCOfferingId")
            .addKdoc("Auto-generated offering ID constants from RevenueCat dashboard.")

        for (offering in offeringsList) {
            val constName = NamingConfig.toConstant(offering.lookupKey)
            objectBuilder.addProperty(
                PropertySpec.builder(constName, String::class)
                    .addModifiers(KModifier.CONST)
                    .initializer("%S", offering.lookupKey)
                    .addKdoc("%L", offering.displayName)
                    .build(),
            )
        }

        FileSpec.builder(packageName, "RCOfferingId")
            .addType(objectBuilder.build())
            .build()
            .writeTo(outputDir)
    }

    private fun generateExtensions(offeringsList: List<OfferingSchema>, outputDir: File) {
        val fileBuilder = FileSpec.builder(packageName, "OfferingsExt")
            .addFileComment("Auto-generated extension properties for Offerings.")

        for (offeringSchema in offeringsList) {
            val propName = NamingConfig.toIdentifier(offeringSchema.lookupKey, namingStyle)

            fileBuilder.addProperty(
                PropertySpec.builder(propName, offering.copy(nullable = true))
                    .receiver(offerings)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return this.getOffering(%S)", offeringSchema.lookupKey)
                            .build(),
                    )
                    .addKdoc("%L", offeringSchema.displayName)
                    .build(),
            )
        }

        fileBuilder.build().writeTo(outputDir)
    }
}
