package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.SubscriptionInformation

@Composable
internal fun SubscriptionDetailsView(
    details: SubscriptionInformation,
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
                overline = "Billing cycle",
                text = details.durationTitle,
            )

            Spacer(modifier = Modifier.size(PaddingVertical))

            SubscriptionDetailRow(
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
                    overline = expirationOverline,
                    text = details.expirationDateString,
                )
            }
        }
    }
}

@Composable
private fun SubscriptionDetailRow(
    overline: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        // TODO Icon
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

private val PaddingVertical = 8.dp
private val PaddingContent = 16.dp
private const val AlphaSecondaryText = 0.6f

private class SubscriptionInformationProvider : PreviewParameterProvider<SubscriptionInformation> {
    override val values: Sequence<SubscriptionInformation> = sequenceOf(
        SubscriptionInformation(
            title = "Basic",
            durationTitle = "Monthly",
            price = "$4.99",
            expirationDateString = "June 1st, 2024",
            productIdentifier = "product_id",
            willRenew = true,
            active = true,
        ),
        SubscriptionInformation(
            title = "Basic",
            durationTitle = "Yearly",
            price = "$49.99",
            expirationDateString = "June 1st, 2024",
            productIdentifier = "product_id",
            willRenew = false,
            active = true,
        ),
        SubscriptionInformation(
            title = "Basic",
            durationTitle = "Weekly",
            price = "$1.99",
            expirationDateString = "June 1st, 2024",
            productIdentifier = "product_id",
            willRenew = false,
            active = false,
        ),
    )
}

@Preview
@Composable
internal fun SubscriptionDetailsView_Preview(
    @PreviewParameter(SubscriptionInformationProvider::class) details: SubscriptionInformation,
) {
    SubscriptionDetailsView(
        details = details,
    )
}
