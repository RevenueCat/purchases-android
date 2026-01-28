package com.revenuecat.purchases.ui.revenuecatui.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.customercenter.CustomerCenterManagementOption
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterOptions
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
    constructor(
        context: Context,
        dismissHandler: (() -> Unit)? = null,
    ) : this(
        context = context,
        customerCenterListener = null,
        dismissHandler = dismissHandler,
    )

    @JvmOverloads
    constructor(
        context: Context,
        customerCenterListener: CustomerCenterListener? = null,
        dismissHandler: (() -> Unit)? = null,
    ) : super(context) {
        this.customerCenterListener = customerCenterListener
        this.dismissHandler = dismissHandler
        init()
    }

    private var dismissHandler: (() -> Unit)? = null
    private var customerCenterListener: CustomerCenterListener? = null
    private val internalListener = object : CustomerCenterListener {
        override fun onRestoreStarted() {
            customerCenterListener?.onRestoreStarted()
        }

        override fun onRestoreFailed(error: PurchasesError) {
            customerCenterListener?.onRestoreFailed(error)
        }

        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            customerCenterListener?.onRestoreCompleted(customerInfo)
        }

        override fun onShowingManageSubscriptions() {
            customerCenterListener?.onShowingManageSubscriptions()
        }

        override fun onFeedbackSurveyCompleted(feedbackSurveyOptionId: String) {
            customerCenterListener?.onFeedbackSurveyCompleted(feedbackSurveyOptionId)
        }

        override fun onManagementOptionSelected(action: CustomerCenterManagementOption) {
            customerCenterListener?.onManagementOptionSelected(action)
        }

        override fun onCustomActionSelected(actionIdentifier: String, purchaseIdentifier: String?) {
            customerCenterListener?.onCustomActionSelected(actionIdentifier, purchaseIdentifier)
        }
    }
    private val customerCenterOptions = CustomerCenterOptions.Builder()
        .setListener(internalListener)
        .build()

    /**
     * Sets a dismiss handler for when the customer center is closed.
     */
    fun setDismissHandler(dismissHandler: (() -> Unit)?) {
        this.dismissHandler = dismissHandler
    }

    /**
     * Sets a [CustomerCenterListener] that will receive callbacks for this instance of the Customer Center.
     * If not provided, callbacks fall back to the listener configured on [com.revenuecat.purchases.Purchases].
     */
    fun setCustomerCenterListener(customerCenterListener: CustomerCenterListener?) {
        this.customerCenterListener = customerCenterListener
    }

    override fun onBackPressed() {
        dismissHandler?.run { dismiss() } ?: super.onBackPressed()
    }

    private fun init() {
        Logger.d("Initialized CustomerCenterView")
    }

    private fun dismiss() {
        dismissHandler?.invoke()
        destroy()
    }

    @Composable
    override fun Content() {
        val isDarkTheme = isSystemInDarkTheme()
        val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

        MaterialTheme(colorScheme = colorScheme) {
            CustomerCenter(options = customerCenterOptions) {
                dismiss()
            }
        }
    }
}
