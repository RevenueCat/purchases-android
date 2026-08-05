package com.revenuecat.e2etests

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class E2ETestsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.DEBUG

        // The workflow E2E flows are built with E2E_WORKFLOWS_API_KEY set (surfaced as
        // BuildConfig.WORKFLOWS_API_KEY). When it's present we defer configuration until the first Activity
        // is created, so a Maestro launch argument can select the initial debug-only failure strategy. The
        // default build configures eagerly, keeping the CI test_store_annual_purchase flow untouched.
        if (BuildConfig.WORKFLOWS_API_KEY != WORKFLOWS_API_KEY_PLACEHOLDER) {
            registerActivityLifecycleCallbacks(ConfigureOnFirstActivity())
        } else {
            configurePurchases(
                PurchasesConfiguration.Builder(context = this, apiKey = Constants.API_KEY).build(),
            )
        }
    }

    private inner class ConfigureOnFirstActivity : ActivityLifecycleCallbacksAdapter() {
        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (!Purchases.isConfigured) {
                configurePurchases(
                    PurchasesConfiguration.Builder(
                        context = this@E2ETestsApplication,
                        apiKey = BuildConfig.WORKFLOWS_API_KEY,
                    ).build(),
                    initialForceServerErrorStrategy = activity.intent?.getStringExtra(FORCE_SERVER_ERROR_EXTRA_KEY),
                )
            }
            unregisterActivityLifecycleCallbacks(this)
        }
    }

    internal companion object {
        private const val WORKFLOWS_API_KEY_PLACEHOLDER = "workflows_api_key_to_replace"
        private const val FORCE_SERVER_ERROR_EXTRA_KEY = "force_server_error_strategy"

        fun forceConfigKillSwitch() {
            armRemoteConfigKillSwitchForE2ETests()
        }
    }
}

/** No-op base so [E2ETestsApplication] only overrides the lifecycle callback it needs. */
private open class ActivityLifecycleCallbacksAdapter : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
