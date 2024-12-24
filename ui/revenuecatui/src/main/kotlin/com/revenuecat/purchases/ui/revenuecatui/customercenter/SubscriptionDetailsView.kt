package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PurchaseInformation
import com.revenuecat.purchases.ui.revenuecatui.icons.CalendarMonth
import com.revenuecat.purchases.ui.revenuecatui.icons.CurrencyExchange
import com.revenuecat.purchases.ui.revenuecatui.icons.UniversalCurrencyAlt

@Composable
internal fun SubscriptionDetailsView(
    details: PurchaseInformation,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .padding(all = PaddingContent),
        ) {
            Text(
                text = details.title,
                style = MaterialTheme.typography.titleMedium,
            )

            val explanation = remember(details.active, details.willRenew) {
                when {
                    details.active && details.willRenew -> "This is your subscription with the earliest billing date."
                    details.active -> "This is your subscription with the earliest expiration date."
                    else -> "This subscription has expired."
                }
            }

            Text(
                text = explanation,
                color = LocalContentColor.current.copy(alpha = AlphaSecondaryText),
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.size(PaddingVertical))

            HorizontalDivider()

            Spacer(modifier = Modifier.size(PaddingVertical))

            SubscriptionDetailRow(
                icon = CurrencyExchange,
                overline = "Billing cycle",
                text = details.durationTitle,
            )

            Spacer(modifier = Modifier.size(PaddingVertical))

            SubscriptionDetailRow(
                icon = UniversalCurrencyAlt,
                overline = "Current price",
                text = details.price,
            )

            if (details.expirationDateString != null) {
                val expirationOverline = remember(details.active, details.willRenew) {
                    when {
                        details.active && details.willRenew -> "Next billing date"
                        details.active -> "Expires"
                        else -> "Expired"
                    }
                }

                Spacer(modifier = Modifier.size(PaddingVertical))

                SubscriptionDetailRow(
                    icon = CalendarMonth,
                    overline = expirationOverline,
                    text = details.expirationDateString,
                )
            }
        }
    }
}

@Composable
private fun SubscriptionDetailRow(
    icon: ImageVector,
    overline: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size((LocalDensity.current.fontScale * SizeIconDp).dp),
        )

        Spacer(modifier = Modifier.size(PaddingHorizontal))

        Column {
            Text(
                text = overline,
                color = LocalContentColor.current.copy(alpha = AlphaSecondaryText),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private const val AlphaSecondaryText = 0.6f
private val PaddingContent = 16.dp
private val PaddingHorizontal = 8.dp
private val PaddingVertical = 8.dp
private const val SizeIconDp = 22

private class SubscriptionInformationProvider : PreviewParameterProvider<PurchaseInformation> {
    override val values: Sequence<PurchaseInformation> = sequenceOf(
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Monthly",
            price = "$4.99",
            expirationDateString = "June 1st, 2024",
            willRenew = true,
            active = true,
            productId = "basic_monthly",
        ),
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Yearly",
            price = "$49.99",
            expirationDateString = "June 1st, 2024",
            willRenew = false,
            active = true,
            productId = "basic_yearly",
        ),
        PurchaseInformation(
            title = "Basic",
            durationTitle = "Weekly",
            price = "$1.99",
            expirationDateString = "June 1st, 2024",
            willRenew = false,
            active = false,
            productId = "basic_weekly",
        ),
    )
}

@Preview(group = "scale = 1", fontScale = 1F)
// Unrealistically long device to make the Column fit. Can be removed once Emerge Snapshot Test supports
// @PreviewParameter.
@Preview(group = "scale = 2", fontScale = 2F, device = "spec:width=1080px,height=4720px,dpi=440")
@Composable
internal fun SubscriptionDetailsView_Preview() {
    Column(
        modifier = Modifier.height(2000.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Bit of a roundabout way of using the PreviewParameterProvider, because Emerge Snapshot Test doesn't support
        // it yet.
        SubscriptionInformationProvider().values.forEach { details ->
            SubscriptionDetailsView(
                details = details,
            )
        }
    }
}
