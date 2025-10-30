package com.revenuecat.purchases.common

import android.content.Context
import com.revenuecat.purchases.APIKeyValidator
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.finishTransactions
import com.revenuecat.purchases.strings.ConfigureStrings
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("LongParameterList")
internal class AppConfig(
    context: Context,
    purchasesAreCompletedBy: PurchasesAreCompletedBy,
    val showInAppMessagesAutomatically: Boolean,
    val platformInfo: PlatformInfo,
    proxyURL: URL?,
    val store: Store,
    val isDebugBuild: Boolean,
    val apiKeyValidationResult: APIKeyValidator.ValidationResult,
    val dangerousSettings: DangerousSettings = DangerousSettings(autoSyncPurchases = true),
    // Should only be used for tests
    val runningTests: Boolean = false,
    forceSigningErrors: Boolean = false,
    baseUrlString: String = Companion.baseUrlString,
) {
    companion object {
        val diagnosticsURL = URL("https://api-diagnostics.revenuecat.com/")
        val paywallEventsURL = URL("https://api-paywalls.revenuecat.com/")
        val fallbackURL = URL("https://api-production.8-lives-cat.io/")
        const val baseUrlString = "https://api.revenuecat.com/"
    }

    // Should only be used for tests
    var forceSigningErrors: Boolean = forceSigningErrors
        get() = runningTests && field

    private val _isAppBackgrounded: AtomicBoolean = AtomicBoolean(true)
    var isAppBackgrounded: Boolean
        get() = _isAppBackgrounded.get()
        set(value) {
            _isAppBackgrounded.set(value)
        }

    val enableOfflineEntitlements = true
    val languageTag: String = context.getLocale()?.toLanguageTag() ?: ""
    val versionName: String = context.versionName ?: ""
    val packageName: String = context.packageName
    var finishTransactions: Boolean = purchasesAreCompletedBy.finishTransactions
    val baseURL: URL = proxyURL?.also {
        log(LogIntent.INFO) { ConfigureStrings.CONFIGURING_PURCHASES_PROXY_URL_SET }
    } ?: URL(baseUrlString)
    val fallbackBaseURLs: List<URL> = if (proxyURL != null) {
        emptyList()
    } else {
        listOf(fallbackURL)
    }
    val customEntitlementComputation: Boolean
        get() = dangerousSettings.customEntitlementComputation

    val playStoreVersionName = context.playStoreVersionName
    val playServicesVersionName = context.playServicesVersionName

    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppConfig

        if (platformInfo != other.platformInfo) return false
        if (store != other.store) return false
        if (isDebugBuild != other.isDebugBuild) return false
        if (dangerousSettings != other.dangerousSettings) return false
        if (languageTag != other.languageTag) return false
        if (versionName != other.versionName) return false
        if (packageName != other.packageName) return false
        if (finishTransactions != other.finishTransactions) return false
        if (forceSigningErrors != other.forceSigningErrors) return false
        if (baseURL != other.baseURL) return false
        if (showInAppMessagesAutomatically != other.showInAppMessagesAutomatically) return false
        if (isAppBackgrounded != other.isAppBackgrounded) return false
        if (apiKeyValidationResult != other.apiKeyValidationResult) return false

        return true
    }

    override fun hashCode(): Int {
        var result = platformInfo.hashCode()
        result = 31 * result + store.hashCode()
        result = 31 * result + isDebugBuild.hashCode()
        result = 31 * result + dangerousSettings.hashCode()
        result = 31 * result + languageTag.hashCode()
        result = 31 * result + versionName.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + finishTransactions.hashCode()
        result = 31 * result + forceSigningErrors.hashCode()
        result = 31 * result + baseURL.hashCode()
        result = 31 * result + showInAppMessagesAutomatically.hashCode()
        result = 31 * result + isAppBackgrounded.hashCode()
        result = 31 * result + apiKeyValidationResult.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppConfig(" +
            "platformInfo=$platformInfo, " +
            "store=$store, " +
            "isDebugBuild=$isDebugBuild, " +
            "dangerousSettings=$dangerousSettings, " +
            "languageTag='$languageTag', " +
            "versionName='$versionName', " +
            "packageName='$packageName', " +
            "finishTransactions=$finishTransactions, " +
            "showInAppMessagesAutomatically=$showInAppMessagesAutomatically, " +
            "apiKeyValidationResult=$apiKeyValidationResult, " +
            "baseURL=$baseURL)"
    }
}
