package com.revenuecat.purchases.common.diagnostics

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

internal class DiagnosticsHelper(
    context: Context,
    private val diagnosticsFileHelper: DiagnosticsFileHelper,
    private val sharedPreferences: Lazy<SharedPreferences> = lazy { initializeSharedPreferences(context) },
) {
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val CONSECUTIVE_FAILURES_COUNT_KEY = "consecutive_failures_count"

        fun initializeSharedPreferences(context: Context): SharedPreferences =
            context.getSharedPreferences(
                "com_revenuecat_purchases_${context.packageName}_preferences_diagnostics",
                Context.MODE_PRIVATE,
            )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun resetDiagnosticsStatus() {
        clearConsecutiveNumberOfErrors()
        diagnosticsFileHelper.deleteFile()
    }

    fun clearConsecutiveNumberOfErrors() {
        sharedPreferences.value.edit().remove(CONSECUTIVE_FAILURES_COUNT_KEY).apply()
    }

    fun increaseConsecutiveNumberOfErrors(): Int {
        var count = sharedPreferences.value.getInt(CONSECUTIVE_FAILURES_COUNT_KEY, 0)
        sharedPreferences.value.edit().putInt(CONSECUTIVE_FAILURES_COUNT_KEY, ++count).apply()
        return count
    }
}
