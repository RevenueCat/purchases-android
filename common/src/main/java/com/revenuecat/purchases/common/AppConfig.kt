package com.revenuecat.purchases.common

import android.content.Context
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.Store
import java.net.URL
import com.revenuecat.purchases.strings.ConfigureStrings

@Suppress("LongParameterList")
class AppConfig(
    context: Context,
    observerMode: Boolean,
    val platformInfo: PlatformInfo,
    proxyURL: URL?,
    val store: Store,
    val dangerousSettings: DangerousSettings = DangerousSettings(autoSyncPurchases = true),
    // Should only be used for tests
    var forceServerErrors: Boolean = false
) {

    val enableOfflineEntitlements = true
    val languageTag: String = context.getLocale()?.toBCP47() ?: ""
    val versionName: String = context.versionName ?: ""
    val packageName: String = context.packageName
    var finishTransactions: Boolean = !observerMode
    val baseURL: URL = proxyURL?.also {
        log(LogIntent.INFO, ConfigureStrings.CONFIGURING_PURCHASES_PROXY_URL_SET)
    } ?: URL("https://api.revenuecat.com/")
    val diagnosticsURL = URL("https://api-diagnostics.revenuecat.com/")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppConfig

        if (platformInfo != other.platformInfo) return false
        if (store != other.store) return false
        if (dangerousSettings != other.dangerousSettings) return false
        if (languageTag != other.languageTag) return false
        if (versionName != other.versionName) return false
        if (packageName != other.packageName) return false
        if (finishTransactions != other.finishTransactions) return false
        if (forceServerErrors != other.forceServerErrors) return false
        if (baseURL != other.baseURL) return false

        return true
    }

    override fun hashCode(): Int {
        var result = platformInfo.hashCode()
        result = 31 * result + store.hashCode()
        result = 31 * result + dangerousSettings.hashCode()
        result = 31 * result + languageTag.hashCode()
        result = 31 * result + versionName.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + finishTransactions.hashCode()
        result = 31 * result + forceServerErrors.hashCode()
        result = 31 * result + baseURL.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppConfig(" +
            "platformInfo=$platformInfo, " +
            "store=$store, " +
            "dangerousSettings=$dangerousSettings, " +
            "languageTag='$languageTag', " +
            "versionName='$versionName', " +
            "packageName='$packageName', " +
            "finishTransactions=$finishTransactions, " +
            "baseURL=$baseURL)"
    }
}
