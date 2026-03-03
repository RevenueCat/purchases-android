package com.revenuecat.purchases.codegen.generator

import com.revenuecat.purchases.codegen.NamingConfig
import com.revenuecat.purchases.codegen.NamingStyle
import com.revenuecat.purchases.codegen.api.EntitlementSchema
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import java.io.File

internal class CustomerInfoExtGenerator(
    private val packageName: String,
    private val namingStyle: NamingStyle
) {

    private val customerInfo = ClassName("com.revenuecat.purchases", "CustomerInfo")

    internal fun generate(entitlements: List<EntitlementSchema>, outputDir: File) {
        val fileBuilder = FileSpec.builder(packageName, "CustomerInfoExt")
            .addFileComment("Auto-generated extension properties for CustomerInfo.")

        for (entitlement in entitlements) {
            val propName = NamingConfig.toUnescapedIdentifier(entitlement.lookupKey, namingStyle)
            val activePropName = NamingConfig.escapeIfReservedKeyword(
                "is${propName.replaceFirstChar { it.uppercase() }}Active"
            )

            fileBuilder.addProperty(
                PropertySpec.builder(activePropName, Boolean::class)
                    .receiver(customerInfo)
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement(
                                "return this.entitlements[%S]?.isActive == true",
                                entitlement.lookupKey
                            )
                            .build()
                    )
                    .addKdoc(
                        "Whether the %L entitlement is currently active for this customer.",
                        entitlement.displayName
                    )
                    .build()
            )
        }

        fileBuilder.build().writeTo(outputDir)
    }
}
