package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.SubscriptionInformation

@Composable
internal fun SubscriptionDetailsView(
    details: SubscriptionInformation,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(text = details.title)

        val explanation = remember(details.active, details.willRenew) {
            when {
                details.active && details.willRenew -> "This is your subscription with the earliest billing date."
                details.active -> "This is your subscription with the earliest expiration date."
                else -> "This subscription has expired."
            }
        }

        Text(text = explanation)

        SubscriptionDetailRow(
            overline = "Billing cycle",
            text = details.durationTitle,
        )
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

            SubscriptionDetailRow(
                overline = expirationOverline,
                text = details.expirationDateString,
            )
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
            Text(text = overline)
            Text(text = text)
        }
    }
}

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
