package com.revenuecat.purchases

import android.content.Context
import android.preference.PreferenceManager
import androidx.test.ext.junit.rules.activityScenarioRule
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.models.StoreTransaction
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import java.net.URL
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

open class BasePurchasesIntegrationTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupClass() {
            if (!canRunIntegrationTests()) {
                error("You need to set required constants in Constants.kt")
            }
        }

        private fun canRunIntegrationTests() = Constants.apiKey != "REVENUECAT_API_KEY" &&
            Constants.googlePurchaseToken != "GOOGLE_PURCHASE_TOKEN" &&
            Constants.productIdToPurchase != "PRODUCT_ID_TO_PURCHASE"
    }

    @get:Rule
    var activityScenarioRule = activityScenarioRule<MainActivity>()

    protected open val initialActivePurchasesToUse: Map<String, StoreTransaction> = emptyMap()
    protected open val initialForceServerErrors: Boolean = false

    protected val testTimeout = 5.seconds
    protected val currentTimestamp = Date().time
    protected val testUserId = "android-integration-test-$currentTimestamp"
    protected val proxyUrl = Constants.proxyUrl.takeIf { it != "NO_PROXY_URL" }

    protected lateinit var mockBillingAbstract: BillingAbstract

    protected var latestPurchasesUpdatedListener: BillingAbstract.PurchasesUpdatedListener? = null
    protected var latestStateListener: BillingAbstract.StateListener? = null

    // We shouldn't cache the whole activity, but considering that these are simple integration tests
    // and we don't perform any configuration changes, it shouldn't cause any leaks.
    protected val activity: MainActivity
        get() = _activity ?: Assertions.fail("Expected activity to not be null")
    private var _activity: MainActivity? = null

    private val eTagsSharedPreferencesNameTemplate = "%s_preferences_etags"
    private val diagnosticsSharedPreferencesNameTemplate = "com_revenuecat_purchases_%s_preferences_diagnostics"

    protected val entitlementsToVerify = Constants.activeEntitlementIdsToVerify
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    @After
    fun tearDown() {
        _activity = null
        Purchases.resetSingleton()
    }

    // region helpers

    protected fun setUpTest(
        initialSharedPreferences: Map<String, String> = emptyMap(),
        initialActivePurchases: Map<String, StoreTransaction> = initialActivePurchasesToUse,
        forceServerErrors: Boolean = initialForceServerErrors,
        postSetupTestCallback: (MainActivity) -> Unit = {},
    ) {
        latestPurchasesUpdatedListener = null
        latestStateListener = null

        onActivityReady {
            _activity = it
            clearAllSharedPreferences(it)
            writeSharedPreferences(it, initialSharedPreferences)

            mockBillingAbstract = mockk<BillingAbstract>(relaxed = true).apply {
                every { purchasesUpdatedListener = any() } answers { latestPurchasesUpdatedListener = firstArg() }
                every { stateListener = any() } answers { latestStateListener = firstArg() }
                every { isConnected() } returns true
            }
            mockActivePurchases(initialActivePurchases)

            proxyUrl?.let { urlString ->
                Purchases.proxyURL = URL(urlString)
            }

            configureSdk(it, forceServerErrors)

            postSetupTestCallback(it)
        }
    }

    protected fun onActivityReady(block: (MainActivity) -> Unit) {
        activityScenarioRule.scenario.onActivity(block)
    }

    protected fun simulateSdkRestart(context: Context, forceServerErrors: Boolean = false) {
        Purchases.resetSingleton()
        configureSdk(context, forceServerErrors)
    }

    protected fun configureSdk(context: Context, forceServerErrors: Boolean = false) {
        Purchases.configure(
            PurchasesConfiguration.Builder(context, Constants.apiKey)
                .appUserID(testUserId)
                .build(),
            mockBillingAbstract,
            forceServerErrors,
        )
    }

    protected fun ensureBlockFinishes(block: (CountDownLatch) -> Unit) {
        val latch = CountDownLatch(1)
        block(latch)
        latch.await(testTimeout.inWholeSeconds, TimeUnit.SECONDS)
        Assertions.assertThat(latch.count).isEqualTo(0)
    }

    protected fun mockActivePurchases(activePurchases: Map<String, StoreTransaction>) {
        val callbackSlot = slot<(Map<String, StoreTransaction>) -> Unit>()
        every {
            mockBillingAbstract.queryPurchases(
                testUserId,
                capture(callbackSlot),
                any(),
            )
        } answers {
            callbackSlot.captured.invoke(activePurchases)
        }
    }

    private fun clearAllSharedPreferences(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        context.getSharedPreferences(
            eTagsSharedPreferencesNameTemplate.format(context.packageName),
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
        context.getSharedPreferences(
            diagnosticsSharedPreferencesNameTemplate.format(context.packageName),
            Context.MODE_PRIVATE,
        ).edit().clear().commit()
    }

    private fun writeSharedPreferences(context: Context, values: Map<String, String>) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        values.forEach { (key, value) ->
            editor.putString(key, value)
        }
        editor.commit()
    }

    // endregion
}
