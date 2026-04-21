package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.Store
import kotlinx.parcelize.Parcelize

private const val SDK_CONFIG_EXTRA = "sdk_config_args"

@Parcelize
internal data class SdkConfigurationState(
    val apiKey: String,
    val appUserId: String?,
    val purchasesAreCompletedBy: PurchasesAreCompletedBy,
    val showInAppMessagesAutomatically: Boolean,
    val store: Store,
    val diagnosticsEnabled: Boolean,
    val verificationMode: EntitlementVerificationMode,
    val dangerousSettings: DangerousSettings,
    val pendingTransactionsForPrepaidPlansEnabled: Boolean,
) : Parcelable {

    companion object {
        fun from(configuration: PurchasesConfiguration): SdkConfigurationState = SdkConfigurationState(
            apiKey = configuration.apiKey,
            appUserId = configuration.appUserID,
            purchasesAreCompletedBy = configuration.purchasesAreCompletedBy,
            showInAppMessagesAutomatically = configuration.showInAppMessagesAutomatically,
            store = configuration.store,
            diagnosticsEnabled = configuration.diagnosticsEnabled,
            verificationMode = configuration.verificationMode,
            dangerousSettings = configuration.dangerousSettings,
            pendingTransactionsForPrepaidPlansEnabled = configuration.pendingTransactionsForPrepaidPlansEnabled,
        )
    }

    fun toConfiguration(context: Context): PurchasesConfiguration {
        return PurchasesConfiguration.Builder(context, apiKey)
            .appUserID(appUserId)
            .purchasesAreCompletedBy(purchasesAreCompletedBy)
            .showInAppMessagesAutomatically(showInAppMessagesAutomatically)
            .store(store)
            .diagnosticsEnabled(diagnosticsEnabled)
            .entitlementVerificationMode(verificationMode)
            .dangerousSettings(dangerousSettings)
            .pendingTransactionsForPrepaidPlansEnabled(pendingTransactionsForPrepaidPlansEnabled)
            .build()
    }
}

private fun Bundle.getSdkConfigurationState(): SdkConfigurationState? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(SDK_CONFIG_EXTRA, SdkConfigurationState::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(SDK_CONFIG_EXTRA)
    }
}

internal fun saveSdkConfiguration(outState: Bundle) {
    if (!Purchases.isConfigured) return
    outState.putParcelable(
        SDK_CONFIG_EXTRA,
        SdkConfigurationState.from(Purchases.sharedInstance.currentConfiguration),
    )
}

internal fun restoreSdkConfigurationIfNeeded(context: Context, savedInstanceState: Bundle?) {
    val sdkConfigArgs = savedInstanceState?.getSdkConfigurationState() ?: return
    val savedConfiguration = sdkConfigArgs.toConfiguration(context)

    if (Purchases.isConfigured) {
        val currentConfiguration = Purchases.sharedInstance.currentConfiguration
        if (currentConfiguration == savedConfiguration) {
            Logger.i("Skipping Purchases reconfiguration, configuration is unchanged.")
        } else {
            Logger.i("Purchases already configured with different parameters; not reconfiguring.")
        }
        return
    }

    Purchases.configure(savedConfiguration)
}
