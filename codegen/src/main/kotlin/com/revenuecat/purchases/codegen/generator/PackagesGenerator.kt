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

internal class PackagesGenerator(
    private val packageName: String,
    private val namingStyle: NamingStyle
) {

    private val offering = ClassName("com.revenuecat.purchases", "Offering")
    private val rcPackage = ClassName("com.revenuecat.purchases", "Package")

    internal fun generate(offeringsList: List<OfferingSchema>, outputDir: File) {
        for (offeringSchema in offeringsList) {
            if (offeringSchema.packages.isEmpty()) continue
            generateForOffering(offeringSchema, outputDir)
        }
    }

    private fun generateForOffering(offeringSchema: OfferingSchema, outputDir: File) {
        // offeringIdent is used as a prefix for extension property names to prevent
        // collisions when multiple offerings share a package lookupKey (e.g. "$rc_monthly").
        val offeringIdent = NamingConfig.toUnescapedIdentifier(offeringSchema.lookupKey, namingStyle)
        val offeringIdentCap = offeringIdent.replaceFirstChar { it.uppercase() }
        val objectName = NamingConfig.escapeIfReservedKeyword("RC${offeringIdentCap}PackageId")
        val fileName = objectName

        val objectBuilder = TypeSpec.objectBuilder(objectName)
            .addKdoc(
                "Auto-generated package ID constants for the %L offering.",
                offeringSchema.displayName
            )

        for (pkg in offeringSchema.packages) {
            val constName = NamingConfig.toConstant(pkg.lookupKey)
            objectBuilder.addProperty(
                PropertySpec.builder(constName, String::class)
                    .addModifiers(KModifier.CONST)
                    .initializer("%S", pkg.lookupKey)
                    .addKdoc("%L", pkg.displayName)
                    .build()
            )
        }

        val fileBuilder = FileSpec.builder(packageName, fileName)
            .addType(objectBuilder.build())

        for (pkg in offeringSchema.packages) {
            // Prefix with offering identifier so that two offerings with the same
            // package lookupKey produce distinct extension names, e.g.:
            //   defaultMonthly  (offering "default",      package "$rc_monthly")
            //   premiumMonthly  (offering "premium",      package "$rc_monthly")
            val pkgIdent = NamingConfig.toUnescapedIdentifier(pkg.lookupKey, namingStyle)
            val propName = NamingConfig.escapeIfReservedKeyword(
                "${offeringIdent}${pkgIdent.replaceFirstChar { it.uppercase() }}"
            )

            fileBuilder.addProperty(
                PropertySpec.builder(propName, rcPackage.copy(nullable = true))
                    .receiver(offering)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return this.getPackage(%S)", pkg.lookupKey)
                            .build()
                    )
                    .addKdoc("%L package from %L offering.", pkg.displayName, offeringSchema.displayName)
                    .build()
            )
        }

        fileBuilder.build().writeTo(outputDir)
    }
}
