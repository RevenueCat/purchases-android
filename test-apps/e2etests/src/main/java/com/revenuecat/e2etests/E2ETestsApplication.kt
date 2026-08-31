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

        // Configure from the first Activity so Maestro launch arguments can select the initial debug-only failure
        // strategy and app locale before the SDK starts. Flows without launch arguments use the default configuration.
        registerActivityLifecycleCallbacks(ConfigurePurchasesOnFirstActivity())
    }

    private inner class ConfigurePurchasesOnFirstActivity : ActivityLifecycleCallbacksAdapter() {
        override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (!Purchases.isConfigured) {
                configurePurchases(
                    PurchasesConfiguration.Builder(
                        context = this@E2ETestsApplication,
                        apiKey = BuildConfig.API_KEY,
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
