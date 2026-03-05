package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import com.revenuecat.purchases.ui.revenuecatui.helpers.PaywallWarning

private val previewPackages: List<Package> = listOf(
    TestData.Packages.annual,
    TestData.Packages.monthly,
)

@Preview(showBackground = true, name = "Fallback Paywall R/G")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Fallback Paywall R/G Dark",
)
@Preview(showBackground = true, locale = "es-rES", name = "Fallback Paywall Spanish")
@Composable
private fun DefaultPaywallRedGreenPreview() {
    DefaultPaywallPreview(
        icon = DualColorImageGenerator.redGreen,
        warning = null,
    )
}

@Preview(showBackground = true, name = "Fallback Paywall B/G")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Fallback Paywall B/G Dark",
)
@Composable
private fun DefaultPaywallBlueGreenPreview() {
    DefaultPaywallPreview(
        icon = DualColorImageGenerator.blueGreen,
        warning = null,
    )
}

@Preview(showBackground = true, name = "Fallback Paywall P/O")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Fallback Paywall P/O Dark",
)
@Composable
private fun DefaultPaywallPurpleOrangePreview() {
    DefaultPaywallPreview(
        icon = DualColorImageGenerator.purpleOrange,
        warning = null,
    )
}

@Preview(showBackground = true, name = "Warning Paywall - localization")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Warning Paywall - localization Dark",
)
@Composable
private fun DefaultPaywallWarningLocalizationPreview() {
    DefaultPaywallPreview(
        icon = DualColorImageGenerator.redGreen,
        warning = PaywallWarning.MissingLocalization,
    )
}

@Preview(showBackground = true, name = "Warning Paywall - no paywall")
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    name = "Warning Paywall - no paywall Dark",
)
@Composable
private fun DefaultPaywallWarningNoPaywallPreview() {
    DefaultPaywallPreview(
        icon = DualColorImageGenerator.purpleOrange,
        warning = PaywallWarning.NoPaywall("WAT"),
    )
}

@Composable
private fun DefaultPaywallPreview(
    icon: DualColorImageGenerator.PreviewAppIcon,
    warning: PaywallWarning?,
) {
    MaterialTheme {
        DefaultPaywallView(
            packages = previewPackages,
            warning = warning,
            onPurchase = {},
            onRestore = {},
            previewOverrides = DefaultPaywallPreviewOverrides(
                appName = "RevenueCat",
                appIconBitmap = icon.bitmap,
                prominentColors = icon.prominentColors,
                isDebugBuild = true,
            ),
        )
    }
}
