package com.revenuecat.purchases

import androidx.test.platform.app.InstrumentationRegistry

object Constants {
    private val args by lazy { InstrumentationRegistry.getArguments() }

    val proxyUrl: String get() = args.getString("TEST_PROXY_URL") ?: ""

    /**
     * The currently active environment config. Set by [BasePurchasesIntegrationTest]
     * before each test based on the test's [BasePurchasesIntegrationTest.environmentConfig].
     *
     * This allows factories and extension functions to read environment-specific values
     * without needing explicit parameters.
     */
    var activeConfig: EnvironmentConfig = EnvironmentConfig.unconfigured()
        internal set

    // Convenience accessors that delegate to activeConfig.
    // Used by factories (StoreProductFactory, StoreTransactionFactory) and extension functions.
    val apiKey: String get() = activeConfig.apiKey
    val googlePurchaseToken: String get() = activeConfig.googlePurchaseToken
    val productIdToPurchase: String get() = activeConfig.productIdToPurchase
    val basePlanIdToPurchase: String get() = activeConfig.basePlanIdToPurchase
    val activeEntitlementIdsToVerify: String get() = activeConfig.activeEntitlementIdsToVerify
    val backendEnvironment: BackendEnvironment get() = activeConfig.backendEnvironment

    // region Per-environment configs

    val production get() = EnvironmentConfig(
        apiKey = args.getString("PRODUCTION_REVENUECAT_API_KEY") ?: "",
        googlePurchaseToken = args.getString("PRODUCTION_GOOGLE_PURCHASE_TOKEN") ?: "",
        productIdToPurchase = args.getString("PRODUCTION_PRODUCT_ID_TO_PURCHASE") ?: "",
        basePlanIdToPurchase = args.getString("PRODUCTION_BASE_PLAN_ID_TO_PURCHASE") ?: "",
        activeEntitlementIdsToVerify = args.getString("PRODUCTION_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY") ?: "",
        backendEnvironment = BackendEnvironment.PRODUCTION,
    )

    val loadShedderUsEast1 get() = EnvironmentConfig(
        apiKey = args.getString("LOAD_SHEDDER_REVENUECAT_API_KEY") ?: "",
        googlePurchaseToken = args.getString("LOAD_SHEDDER_GOOGLE_PURCHASE_TOKEN") ?: "",
        productIdToPurchase = args.getString("LOAD_SHEDDER_PRODUCT_ID_TO_PURCHASE") ?: "",
        basePlanIdToPurchase = args.getString("LOAD_SHEDDER_BASE_PLAN_ID_TO_PURCHASE") ?: "",
        activeEntitlementIdsToVerify = args.getString("LOAD_SHEDDER_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY") ?: "",
        backendEnvironment = BackendEnvironment.LOAD_SHEDDER_US_EAST_1,
    )

    val loadShedderUsEast2 get() = EnvironmentConfig(
        apiKey = args.getString("LOAD_SHEDDER_REVENUECAT_API_KEY") ?: "",
        googlePurchaseToken = args.getString("LOAD_SHEDDER_GOOGLE_PURCHASE_TOKEN") ?: "",
        productIdToPurchase = args.getString("LOAD_SHEDDER_PRODUCT_ID_TO_PURCHASE") ?: "",
        basePlanIdToPurchase = args.getString("LOAD_SHEDDER_BASE_PLAN_ID_TO_PURCHASE") ?: "",
        activeEntitlementIdsToVerify = args.getString("LOAD_SHEDDER_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY") ?: "",
        backendEnvironment = BackendEnvironment.LOAD_SHEDDER_US_EAST_2,
    )

    val customEntitlementComputation get() = EnvironmentConfig(
        apiKey = args.getString("CEC_REVENUECAT_API_KEY") ?: "",
        googlePurchaseToken = args.getString("CEC_GOOGLE_PURCHASE_TOKEN") ?: "",
        productIdToPurchase = args.getString("CEC_PRODUCT_ID_TO_PURCHASE") ?: "",
        basePlanIdToPurchase = args.getString("CEC_BASE_PLAN_ID_TO_PURCHASE") ?: "",
        activeEntitlementIdsToVerify = args.getString("CEC_ACTIVE_ENTITLEMENT_IDS_TO_VERIFY") ?: "",
        backendEnvironment = BackendEnvironment.PRODUCTION,
    )

    // endregion

    @Suppress("ForbiddenPublicDataClass")
    data class EnvironmentConfig(
        val apiKey: String,
        val googlePurchaseToken: String,
        val productIdToPurchase: String,
        val basePlanIdToPurchase: String,
        val activeEntitlementIdsToVerify: String,
        val backendEnvironment: BackendEnvironment,
    ) {
        companion object {
            fun unconfigured() = EnvironmentConfig(
                apiKey = "",
                googlePurchaseToken = "",
                productIdToPurchase = "",
                basePlanIdToPurchase = "",
                activeEntitlementIdsToVerify = "",
                backendEnvironment = BackendEnvironment.PRODUCTION,
            )
        }
    }

    enum class BackendEnvironment {
        PRODUCTION,
        LOAD_SHEDDER_US_EAST_1,
        LOAD_SHEDDER_US_EAST_2,
    }
}
