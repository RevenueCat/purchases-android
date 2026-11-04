package com.revenuecat.e2etests.workflow

import android.app.Dialog
import android.content.Context
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.ui.revenuecatui.views.PaywallView

private const val WORKFLOW_OFFERING_ID = "default_workflows"

/**
 * Presents the workflow paywall in a [PaywallView] inside a full-screen [Dialog], and presents the exit offering in
 * a new [Dialog] when the paywall reports one. This mirrors the Unity SDK's presenter and exercises
 * [PaywallView.setExitOfferHandler] with the Dialog lifecycle: the Dialog's window has no lifecycle owner, so
 * [PaywallView] installs its own.
 */
@Composable
fun PaywallViewExitOfferScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var offering by remember { mutableStateOf<Offering?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    val presenter = remember(context) { PaywallViewDialogPresenter(context) { dismissed = true } }

    LaunchedEffect(Unit) {
        try {
            offering = Purchases.sharedInstance.awaitOfferings().getOffering(WORKFLOW_OFFERING_ID)
            if (offering == null) error = "Offering '$WORKFLOW_OFFERING_ID' not found"
        } catch (e: PurchasesException) {
            error = e.message ?: "Failed to load offerings"
        }
    }
    DisposableEffect(presenter) { onDispose { presenter.dismiss() } }

    val loaded = offering
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = "PaywallView exit offer", style = MaterialTheme.typography.headlineMedium)
        if (dismissed) Text("Paywall dismissed")
        when {
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            loaded != null -> Button(
                onClick = {
                    dismissed = false
                    presenter.present(loaded.identifier)
                },
            ) { Text("Present Paywall") }
            else -> CircularProgressIndicator()
        }
    }
}

@OptIn(InternalRevenueCatAPI::class)
private class PaywallViewDialogPresenter(
    private val context: Context,
    private val onDismissed: () -> Unit,
) {
    private var currentDialog: Dialog? = null

    fun present(offeringId: String) {
        if (currentDialog != null) return

        val dialog = Dialog(context, android.R.style.Theme_Light_NoTitleBar).apply {
            window?.apply {
                addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            setCancelable(false)
        }
        currentDialog = dialog

        val paywallView = PaywallView(context)
        paywallView.setOfferingId(offeringId)
        paywallView.setDismissHandler {
            dismiss()
            onDismissed()
        }
        paywallView.setExitOfferHandler { exitOffering ->
            // Ignore a repeated close on a paywall that was already replaced. The new paywall gets the same handler,
            // so exit offers chain.
            if (currentDialog === dialog) {
                dismiss()
                present(exitOffering.identifier)
            }
        }
        dialog.setContentView(
            FrameLayout(context).apply {
                addView(
                    paywallView,
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
            },
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        dialog.show()
    }

    fun dismiss() {
        val dialog = currentDialog
        currentDialog = null
        dialog?.dismiss()
    }
}
