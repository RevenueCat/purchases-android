package com.revenuecat.purchases

import androidx.test.platform.app.InstrumentationRegistry

object Constants {
    private val args by lazy { InstrumentationRegistry.getArguments() }

    // Instrumentation args are always set by build.gradle.kts (even to empty).
    // Use the value as-is; empty means "not configured" for required fields,
    // or "intentionally empty" for optional fields like activeEntitlementIdsToVerify.
    val apiKey: String get() = args.getString("REVENUECAT_API_KEY") ?: ""
    val proxyUrl: String get() = args.getString("TEST_PROXY_URL") ?: ""
    val googlePurchaseToken: String get() = args.getString("GOOGLE_PURCHASE_TOKEN") ?: ""
    val productIdToPurchase: String get() = args.getString("PRODUCT_ID_TO_PURCHASE") ?: ""
    val basePlanIdToPurchase: String get() = args.getString("BASE_PLAN_ID_TO_PURCHASE") ?: ""

    // comma separated list of active entitlements to verify (empty means none expected)
    val activeEntitlementIdsToVerify: String get() =
        args.getString("ACTIVE_ENTITLEMENT_IDS_TO_VERIFY") ?: ""

    private val backendEnvironmentString: String get() =
        args.getString("TEST_BACKEND_ENVIRONMENT")?.takeIf { it.isNotEmpty() }
            ?: "production"
    val backendEnvironment: BackendEnvironment get() = BackendEnvironment.valueForString(backendEnvironmentString)

    enum class BackendEnvironment {
        PRODUCTION,
        LOAD_SHEDDER_US_EAST_1,
        LOAD_SHEDDER_US_EAST_2,
        ;

        companion object {
            fun valueForString(backendEnvironmentString: String): BackendEnvironment {
                return when (backendEnvironmentString) {
                    "load_shedder_us_east_1" -> LOAD_SHEDDER_US_EAST_1
                    "load_shedder_us_east_2" -> LOAD_SHEDDER_US_EAST_2
                    "production", "custom_entitlement_computation" -> PRODUCTION
                    else -> error("Expected valid backend_environment value. Got $backendEnvironmentString")
                }
            }
        }
    }
}
