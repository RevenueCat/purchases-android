package com.revenuecat.purchases

object Constants {
    const val apiKey = "REVENUECAT_API_KEY"
    const val proxyUrl = "NO_PROXY_URL"
    const val googlePurchaseToken = "GOOGLE_PURCHASE_TOKEN"
    const val productIdToPurchase = "PRODUCT_ID_TO_PURCHASE"
    const val basePlanIdToPurchase = "BASE_PLAN_ID_TO_PURCHASE"

    // comma separated list of active entitlements to verify
    const val activeEntitlementIdsToVerify = "ACTIVE_ENTITLEMENT_IDS_TO_VERIFY"

    private const val testSuiteString = "TEST_SUITE_INTEGRATION_TESTS"
    val testSuite: TestSuite = TestSuite.valueForString(testSuiteString)

    enum class TestSuite {
        PRODUCTION,
        LOAD_SHEDDER,
        ;

        companion object {
            fun valueForString(testSuiteString: String): TestSuite {
                return when (testSuiteString) {
                    "loadshedder" -> LOAD_SHEDDER
                    "production" -> PRODUCTION
                    else -> error("Expected valid test suite value. Got $testSuiteString")
                }
            }
        }
    }
}
