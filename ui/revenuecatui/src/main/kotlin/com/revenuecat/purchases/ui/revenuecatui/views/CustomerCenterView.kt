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
        dismissHandler: (CustomerCenterView) -> Unit,
    ) : super(context) {
        this.dismissHandler = dismissHandler
    }

    private var dismissHandler: ((CustomerCenterView) -> Unit) = {}

    @Composable
    override fun Content() {
        CustomerCenter {
            dismissHandler(this)
        }
    }
}
