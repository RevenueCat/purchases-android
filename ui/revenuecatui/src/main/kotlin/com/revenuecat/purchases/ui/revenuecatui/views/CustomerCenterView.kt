package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter

/**
 * View that wraps the [CustomerCenter] Composable to display the Customer Center through the View system.
 */
class CustomerCenterView : AbstractComposeView {

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(
        context: Context,
        dismissHandler: () -> Unit,
    ) : super(context) {
        setDismissHandler(dismissHandler)
    }

    private var dismissHandler: (() -> Unit)? = null

    /**
     * Sets a dismiss handler which will be called:
     * - If the user taps on the close button
     * - If the user calls the back button with the Customer Center present.
     */
    fun setDismissHandler(dismissHandler: (() -> Unit)?) {
        this.dismissHandler = dismissHandler
    }

    @Composable
    override fun Content() {
        dismissHandler?.let {
            CustomerCenter(onDismiss = it)
        }
    }
}
