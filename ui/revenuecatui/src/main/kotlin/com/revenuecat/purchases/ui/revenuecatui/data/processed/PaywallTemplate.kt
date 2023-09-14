package com.revenuecat.purchases.ui.revenuecatui.data.processed

internal enum class PaywallTemplate(val id: String, val configurationType: PackageConfigurationType) {
    TEMPLATE_1("1", PackageConfigurationType.SINGLE),
    TEMPLATE_2("2", PackageConfigurationType.MULTIPLE),
    TEMPLATE_3("3", PackageConfigurationType.SINGLE),
    TEMPLATE_4("4_disabled", PackageConfigurationType.MULTIPLE),
    TEMPLATE_5("5_disabled", PackageConfigurationType.MULTIPLE),
    ;

    companion object {
        @Suppress("UnusedParameter")
        fun fromId(id: String): PaywallTemplate? {
            // TODO-PAYWALLS: Uncomment this to support other templates
            // return values().find { it.id == id }
            return TEMPLATE_2
        }
    }
}
