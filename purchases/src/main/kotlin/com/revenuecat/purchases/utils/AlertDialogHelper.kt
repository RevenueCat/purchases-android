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
        neutralButtonText: String,
        onPositiveButtonClicked: () -> Unit,
        onNegativeButtonClicked: () -> Unit,
        onNeutralButtonClicked: () -> Unit,
    )
}

internal class DefaultAlertDialogHelper : AlertDialogHelper {

    override fun showDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String,
        neutralButtonText: String,
        onPositiveButtonClicked: () -> Unit,
        onNegativeButtonClicked: () -> Unit,
        onNeutralButtonClicked: () -> Unit,
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
            .setNeutralButton(neutralButtonText) { dialog, _ ->
                dialog.dismiss()
                onNeutralButtonClicked()
            }
            .show()
    }
}
