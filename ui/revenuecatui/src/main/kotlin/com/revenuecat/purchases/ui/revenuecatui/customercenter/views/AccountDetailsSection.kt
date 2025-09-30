package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.getColorForTheme
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

@Composable
internal fun AccountDetailsSection(
    appUserId: String?,
    appearance: CustomerCenterConfigData.Appearance,
    modifier: Modifier = Modifier,
    originalPurchaseDate: String? = null,
    onCopy: (String) -> Unit = {},
) {
    val hasUserId = !appUserId.isNullOrBlank()
    val hasOriginalPurchaseDate = !originalPurchaseDate.isNullOrBlank()

    if (!hasUserId && !hasOriginalPurchaseDate) {
        return
    }

    val clipboard = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()
    val textColor = appearance.getColorForTheme(isDark) { it.textColor } ?: MaterialTheme.colorScheme.onSurface
    val secondaryColor = textColor.copy(alpha = 0.7f)
    val onCopyClick = appUserId?.let { userId ->
        {
            clipboard.setText(AnnotatedString(userId))
            onCopy(userId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CustomerCenterConstants.Layout.HORIZONTAL_PADDING)
            .padding(top = CustomerCenterConstants.Layout.SECTION_SPACING),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "Account details",
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            modifier = Modifier.padding(bottom = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING),
        )

        AccountDetailsCard(
            hasUserId = hasUserId,
            hasOriginalPurchaseDate = hasOriginalPurchaseDate,
            textColor = textColor,
            secondaryColor = secondaryColor,
            onCopy = onCopyClick,
            appUserId = appUserId,
            originalPurchaseDate = originalPurchaseDate,
        )
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun AccountDetailsCard(
    hasUserId: Boolean,
    hasOriginalPurchaseDate: Boolean,
    textColor: Color,
    secondaryColor: Color,
    onCopy: (() -> Unit)?,
    appUserId: String?,
    originalPurchaseDate: String?,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onCopy != null) {
                onCopy?.invoke()
            },
        shape = RoundedCornerShape(CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CustomerCenterConstants.Card.CARD_PADDING,
                    vertical = 12.dp,
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (onCopy != null) COPY_ICON_SPACE else 0.dp),
                verticalArrangement = Arrangement.spacedBy(CustomerCenterConstants.Layout.ITEMS_SPACING),
            ) {
                if (hasUserId) {
                    Text(
                        text = "App user ID",
                        style = MaterialTheme.typography.labelLarge,
                        color = secondaryColor,
                    )

                    SelectionContainer {
                        Text(
                            text = appUserId!!,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = textColor,
                        )
                    }
                }

                if (hasOriginalPurchaseDate) {
                    Text(
                        text = "Original purchase date",
                        style = MaterialTheme.typography.labelLarge,
                        color = secondaryColor,
                    )

                    Text(
                        text = originalPurchaseDate!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
            }

            if (onCopy != null) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = textColor,
                    )
                }
            }
        }
    }
}

private val COPY_ICON_SPACE = 48.dp

@Preview(showBackground = true)
@Composable
private fun AccountDetailsSectionPreview() {
    CustomerCenterPreviewTheme {
        AccountDetailsSection(
            appUserId = "user-123",
            appearance = CustomerCenterConfigData.Appearance(),
            originalPurchaseDate = "May 4, 2024",
        )
    }
}
