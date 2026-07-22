package com.revenuecat.e2etests

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.ForceServerErrorMode
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class E2ETestsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.DEBUG

        // The workflow E2E flows are built with E2E_WORKFLOWS_API_KEY set (surfaced as
        // BuildConfig.WORKFLOWS_API_KEY). That build defers configure to onActivityPreCreated so the
        // force_server_error_strategy Maestro launch argument (delivered as an intent extra on the
        // first Activity, unreadable here) can shape DangerousSettings. The default build configures
        // eagerly, keeping the CI test_store_annual_purchase flow untouched. Configure stays in the
        // Application either way.
        if (BuildConfig.WORKFLOWS_API_KEY != WORKFLOWS_API_KEY_PLACEHOLDER) {
            registerActivityLifecycleCallbacks(ConfigureOnFirstActivity())
        } else {
            Purchases.configure(
                PurchasesConfiguration.Builder(context = this, apiKey = Constants.API_KEY).build(),
            )
        }
    }

    private inner class ConfigureOnFirstActivity : ActivityLifecycleCallbacksAdapter() {
        @OptIn(InternalRevenueCatAPI::class)
        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (Purchases.isConfigured) return
            val mode = when (activity.intent?.getStringExtra(FORCE_SERVER_ERROR_EXTRA_KEY)) {
                "remote_config_not_found" -> ForceServerErrorMode.REMOTE_CONFIG_NOT_FOUND
                "remote_config_network_error" -> ForceServerErrorMode.REMOTE_CONFIG_NETWORK_ERROR
                else -> null
            }
            Purchases.configure(
                PurchasesConfiguration.Builder(
                    context = this@E2ETestsApplication,
                    apiKey = BuildConfig.WORKFLOWS_API_KEY,
                )
                    .dangerousSettings(DangerousSettings.forWorkflows(forceServerErrorMode = mode))
                    .build(),
            )
        }
    }

    private companion object {
        const val WORKFLOWS_API_KEY_PLACEHOLDER = "workflows_api_key_to_replace"
        const val FORCE_SERVER_ERROR_EXTRA_KEY = "force_server_error_strategy"
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
