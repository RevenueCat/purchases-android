package com.revenuecat.purchases.ui.revenuecatui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI

/**
 * Contract that specifies how to launch the paywall activity and how to parse its result, abstracting
 * that logic from the caller.
 */
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
internal class PaywallContract : ActivityResultContract<PaywallActivityArgs, PaywallResult>() {
    override fun createIntent(context: Context, args: PaywallActivityArgs): Intent {
        return Intent(context, PaywallActivity::class.java).apply {
            putExtra(PaywallActivity.ARGS_EXTRA, args)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaywallResult {
        if (resultCode != Activity.RESULT_OK || intent == null) {
            return PaywallResult.Cancelled
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(PaywallActivity.RESULT_EXTRA, PaywallResult::class.java)
        } else {
            intent.getParcelableExtra(PaywallActivity.RESULT_EXTRA) as? PaywallResult
        }
        return result ?: PaywallResult.Error(
            PurchasesError(PurchasesErrorCode.UnknownError, "PaywallActivity returned null result"),
        )
    }
}
