package com.revenuecat.samplecat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.model.PurchasableState
import com.revenuecat.samplecat.ui.theme.AccentColor
import com.revenuecat.samplecat.ui.theme.SampleCatTheme

/**
 * A card component for displaying and purchasing a product or package.
 *
 * @param title The display title of the product/package
 * @param productId The product identifier
 * @param description A description of the product
 * @param state The current purchase state
 * @param onPurchaseClick Called when the purchase button is clicked
 */
@Composable
fun PurchasableCard(
    title: String?,
    productId: String,
    description: String,
    state: PurchasableState,
    onPurchaseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackground = if (isDarkTheme) Color.Black else Color.White

    val buttonText = when (state) {
        PurchasableState.ReadyToPurchase,
        PurchasableState.PurchasingOtherProduct,
        -> stringResource(R.string.purchase_button)
        PurchasableState.Purchasing -> stringResource(R.string.purchase_button_purchasing)
        PurchasableState.Purchased -> stringResource(R.string.purchase_button_purchased)
    }

    val isEnabled = state == PurchasableState.ReadyToPurchase

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title ?: productId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = productId,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPurchaseClick,
            enabled = isEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentColor,
                contentColor = Color.White,
                disabledContainerColor = AccentColor.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.7f),
            ),
            shape = CircleShape,
            modifier = Modifier.height(36.dp),
        ) {
            if (state == PurchasableState.Purchased) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = buttonText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PurchasableCardReadyPreview() {
    SampleCatTheme {
        PurchasableCard(
            title = stringResource(R.string.preview_subscription_title),
            productId = stringResource(R.string.preview_product_id),
            description = stringResource(R.string.preview_subscription_price),
            state = PurchasableState.ReadyToPurchase,
            onPurchaseClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PurchasableCardPurchasingPreview() {
    SampleCatTheme {
        PurchasableCard(
            title = stringResource(R.string.preview_subscription_title),
            productId = stringResource(R.string.preview_product_id),
            description = stringResource(R.string.preview_subscription_price),
            state = PurchasableState.Purchasing,
            onPurchaseClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PurchasableCardPurchasedPreview() {
    SampleCatTheme {
        PurchasableCard(
            title = stringResource(R.string.preview_subscription_title),
            productId = stringResource(R.string.preview_product_id),
            description = stringResource(R.string.preview_subscription_price),
            state = PurchasableState.Purchased,
            onPurchaseClick = {},
        )
    }
}
