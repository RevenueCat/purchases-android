package com.revenuecat.purchases

object Constants {
    const val apiKey = "REVENUECAT_API_KEY"
    const val proxyUrl = "NO_PROXY_URL"
    const val googlePurchaseToken = "GOOGLE_PURCHASE_TOKEN"
    const val productIdToPurchase = "PRODUCT_ID_TO_PURCHASE"
    const val basePlanIdToPurchase = "BASE_PLAN_ID_TO_PURCHASE"

    // comma separated list of active entitlements to verify
    const val activeEntitlementIdsToVerify = "ACTIVE_ENTITLEMENT_IDS_TO_VERIFY"

    private const val testSuiteString = "TEST_BACKEND_ENVIRONMENT_INTEGRATION_TESTS"
    val testBackendEnvironment: TestBackendEnvironment = TestBackendEnvironment.valueForString(testSuiteString)

    enum class TestBackendEnvironment {
        PRODUCTION,
        LOAD_SHEDDER,
        ;

        companion object {
            fun valueForString(testBackendEnvironmentString: String): TestBackendEnvironment {
                return when (testBackendEnvironmentString) {
                    "loadshedder" -> LOAD_SHEDDER
                    "production" -> PRODUCTION
                    else -> error("Expected valid test backend_environment value. Got $testBackendEnvironmentString")
                }
            }
        }
    }
}
