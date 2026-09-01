package com.revenuecat.purchases.ui.revenuecatui.components.webview

import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue

/** Every field is required, so a section added later cannot be left empty at a call site. */
internal data class WebViewContextInput(
    val customVariables: Map<String, CustomVariableValue>,
    val offering: Offering?,
    /** Its own when the component sits inside a package, else the selection. */
    val componentPackage: Package?,
    val selectedPackage: Package?,
    val store: Store,
    val storefrontCountryCode: String?,
    /** A BCP-47 tag. The content SDK feeds it to `Intl`, which rejects the underscored form. */
    val locale: String,
    val darkMode: Boolean,
)
