package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

/**
 * View that wraps the [CustomerCenter] Composable to display the Customer Center through the View system.
 */
public class CustomerCenterView : CompatComposeView {

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    /**
     * Constructor for programmatic use.
     */
    @JvmOverloads
    constructor(
        context: Context,
        dismissHandler: (() -> Unit)? = null,
    ) : super(context) {
        this.dismissHandler = dismissHandler
        init()
    }

    private var dismissHandler: (() -> Unit)? = null

    /**
     * Sets a dismiss handler for when the customer center is closed.
     */
    fun setDismissHandler(dismissHandler: (() -> Unit)?) {
        this.dismissHandler = dismissHandler
    }

    override fun onBackPressed() {
        dismissHandler?.run { invoke() } ?: super.onBackPressed()
    }

    private fun init() {
        Logger.d("Initialized CustomerCenterView")
    }

    @Composable
    override fun Content() {
        CustomerCenterUI(dismissHandler = dismissHandler)
    }

    @Composable
    private fun CustomerCenterUI(dismissHandler: (() -> Unit)?) {
        CustomerCenter {
            dismissHandler?.invoke()
        }
    }
}
