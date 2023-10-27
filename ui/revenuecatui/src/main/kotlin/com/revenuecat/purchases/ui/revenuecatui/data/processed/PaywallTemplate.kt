package com.revenuecat.purchases.ui.revenuecatui.data.processed

internal enum class PaywallTemplate(val id: String, val configurationType: PackageConfigurationType) {
    TEMPLATE_1("1", PackageConfigurationType.SINGLE),
    TEMPLATE_2("2", PackageConfigurationType.MULTIPLE),
    TEMPLATE_3("3", PackageConfigurationType.SINGLE),
    TEMPLATE_4("4", PackageConfigurationType.MULTIPLE),
    TEMPLATE_5("5", PackageConfigurationType.MULTIPLE),
    ;

    companion object {
        fun fromId(id: String): PaywallTemplate? {
            return values().find { it.id == id }
        }
    }
}
