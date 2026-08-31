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
        // is created, so Maestro launch arguments can select the initial debug-only failure strategy and apply
        // the app locale right after configure. The default build configures eagerly, keeping the CI
        // test_store_annual_purchase flow untouched.
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
                // The paywall resolves its localization from the Activity's Configuration.locales, which
                // Locale.setDefault does not change. overridePreferredUILocale takes priority over the
                // device locales every time the paywall resolves a localization, so it survives the
                // Configuration-driven state updates the workflow paywall performs on each composition.
                activity.intent?.getStringExtra(APP_LOCALE_EXTRA_KEY)?.let { tag ->
                    Purchases.sharedInstance.overridePreferredUILocale(tag)
                }
            }
            unregisterActivityLifecycleCallbacks(this)
        }
    }

    internal companion object {
        private const val WORKFLOWS_API_KEY_PLACEHOLDER = "workflows_api_key_to_replace"
        private const val APP_LOCALE_EXTRA_KEY = "app_locale"
        private const val FORCE_SERVER_ERROR_EXTRA_KEY = "force_server_error_strategy"
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
