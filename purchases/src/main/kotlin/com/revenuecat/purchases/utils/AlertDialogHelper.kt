package com.revenuecat.purchases.utils

import android.app.Activity
import android.app.AlertDialog

internal interface AlertDialogHelper {
    @Suppress("LongParameterList")
    fun showDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String,
        onPositiveButtonClicked: () -> Unit,
        onNegativeButtonClicked: () -> Unit,
    )
}

internal class DefaultAlertDialogHelper : AlertDialogHelper {

    override fun showDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String,
        onPositiveButtonClicked: () -> Unit,
        onNegativeButtonClicked: () -> Unit,
    ) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { dialog, _ ->
                dialog.dismiss()
                onPositiveButtonClicked()
            }
            .setNegativeButton(negativeButtonText) { dialog, _ ->
                dialog.dismiss()
                onNegativeButtonClicked()
            }
            .show()
    }
}
