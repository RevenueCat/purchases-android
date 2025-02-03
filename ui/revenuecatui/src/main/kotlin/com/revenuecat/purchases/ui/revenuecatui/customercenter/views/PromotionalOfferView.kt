@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.ui.revenuecatui.composables.AppIcon
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.getColorForTheme
import com.revenuecat.purchases.ui.revenuecatui.utils.previewSubscriptionOption

@JvmSynthetic
@Composable
internal fun PromotionalOfferView(
    promotionalOfferData: PromotionalOfferData,
    appearance: CustomerCenterConfigData.Appearance,
    onAccept: (SubscriptionOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val textColor = appearance.getColorForTheme(isDark) { it.textColor }
    val buttonBackgroundColor = appearance.getColorForTheme(isDark) { it.buttonBackgroundColor }
    val buttonTextColor = appearance.getColorForTheme(isDark) { it.buttonTextColor }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        AppIcon(
            modifier = Modifier
                .padding(top = 48.dp, bottom = 16.dp)
                .size(100.dp),
        )

        Text(
            text = promotionalOfferData.configuredPromotionalOffer.title,
            style = MaterialTheme.typography.headlineLarge,
            color = textColor ?: MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = promotionalOfferData.configuredPromotionalOffer.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor ?: MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                onAccept(promotionalOfferData.subscriptionOption)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonBackgroundColor ?: MaterialTheme.colorScheme.primary,
                contentColor = buttonTextColor ?: MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(promotionalOfferData.localizedPricingPhasesDescription)
        }
        OutlinedButton(
            onClick = { onDismiss() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = buttonTextColor ?: MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text("No Thanks")
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun PromotionalOfferViewPreview() {
    val promoOffer = CustomerCenterConfigTestData.customerCenterData()
        .getManagementScreen()?.paths?.first {
            it.type == CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST
        }!!.promotionalOffer!!
    val data = PromotionalOfferData(
        promoOffer,
        subscriptionOption = previewSubscriptionOption(
            "rc-cancel-offer",
            "monthly",
        ),
        originalPath = CustomerCenterConfigData.HelpPath(
            id = "1",
            title = "Didn't receive purchase",
            type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
        ),
        localizedPricingPhasesDescription = "1 month for $7.99, then $9.99/mth",
    )
    PromotionalOfferView(
        data,
        appearance = CustomerCenterConfigTestData.customerCenterData().appearance,
        onAccept = {},
        onDismiss = {},
    )
}
