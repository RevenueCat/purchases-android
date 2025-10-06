package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.BuildConfig
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.getColorForTheme

@Composable
internal fun AccountDetailsSection(
    appUserId: String,
    appearance: CustomerCenterConfigData.Appearance,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
    originalPurchaseDate: String? = null,
) {
    if (BuildConfig.DEBUG) {
        DebugAccountDetailsSection(
            appUserId = appUserId,
            appearance = appearance,
            localization = localization,
            modifier = modifier,
            originalPurchaseDate = originalPurchaseDate,
        )
    } else {
        originalPurchaseDate?.let {
            ReleaseAccountDetailsSection(
                originalPurchaseDate = it,
                appearance = appearance,
                localization = localization,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun DebugAccountDetailsSection(
    appUserId: String,
    appearance: CustomerCenterConfigData.Appearance,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
    originalPurchaseDate: String? = null,
) {
    if (appUserId.isBlank() && originalPurchaseDate.isNullOrBlank()) {
        return
    }

    val isDark = isSystemInDarkTheme()
    val textColor = appearance.getColorForTheme(isDark) { it.textColor } ?: MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CustomerCenterConstants.Layout.HORIZONTAL_PADDING)
            .padding(top = CustomerCenterConstants.Layout.SECTION_SPACING),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.ACCOUNT_DETAILS,
            ),
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            modifier = Modifier.padding(bottom = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING),
        )

        DebugAccountDetailsCard(
            appUserId = appUserId,
            originalPurchaseDate = originalPurchaseDate,
            localization = localization,
            textColor = textColor,
        )
    }
}

@Composable
private fun DebugAccountDetailsCard(
    appUserId: String,
    originalPurchaseDate: String?,
    localization: CustomerCenterConfigData.Localization,
    textColor: Color,
) {
    val secondaryColor = textColor.copy(alpha = 0.7f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CustomerCenterConstants.Card.CARD_PADDING,
                    vertical = 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(CustomerCenterConstants.Layout.ITEMS_SPACING),
        ) {
            originalPurchaseDate?.let { purchaseDate ->
                PurchaseDateSection(
                    purchaseDate = purchaseDate,
                    localization = localization,
                    textColor = textColor,
                    secondaryColor = secondaryColor,
                    showSpacer = appUserId.isNotBlank(),
                )
            }

            if (appUserId.isNotBlank()) {
                UserIdSection(
                    appUserId = appUserId,
                    localization = localization,
                    textColor = textColor,
                    secondaryColor = secondaryColor,
                )
            }
        }
    }
}

@Composable
private fun PurchaseDateSection(
    purchaseDate: String,
    localization: CustomerCenterConfigData.Localization,
    textColor: Color,
    secondaryColor: Color,
    showSpacer: Boolean,
) {
    Column {
        Text(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.DATE_WHEN_APP_WAS_PURCHASED,
            ),
            style = MaterialTheme.typography.labelLarge,
            color = secondaryColor,
        )

        Text(
            text = purchaseDate,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
        )

        if (showSpacer) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UserIdSection(
    appUserId: String,
    localization: CustomerCenterConfigData.Localization,
    textColor: Color,
    secondaryColor: Color,
) {
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.USER_ID,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = secondaryColor,
            )

            SelectionContainer {
                Text(
                    text = appUserId,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = textColor,
                )
            }
        }

        IconButton(
            onClick = {
                clipboard.setText(AnnotatedString(appUserId))
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.COPY_TITLE,
                ),
                tint = textColor,
            )
        }
    }
}

@Composable
private fun ReleaseAccountDetailsSection(
    originalPurchaseDate: String,
    appearance: CustomerCenterConfigData.Appearance,
    localization: CustomerCenterConfigData.Localization,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val textColor = appearance.getColorForTheme(isDark) { it.textColor } ?: MaterialTheme.colorScheme.onSurface
    val secondaryColor = textColor.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CustomerCenterConstants.Layout.HORIZONTAL_PADDING)
            .padding(top = CustomerCenterConstants.Layout.SECTION_SPACING),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = localization.commonLocalizedString(
                CustomerCenterConfigData.Localization.CommonLocalizedString.ACCOUNT_DETAILS,
            ),
            style = MaterialTheme.typography.titleLarge,
            color = textColor,
            modifier = Modifier.padding(bottom = CustomerCenterConstants.Layout.SECTION_TITLE_BOTTOM_PADDING),
        )

        OriginalPurchaseDateCard(
            originalPurchaseDate = originalPurchaseDate,
            localization = localization,
            textColor = textColor,
            secondaryColor = secondaryColor,
        )
    }
}

@Composable
private fun OriginalPurchaseDateCard(
    originalPurchaseDate: String,
    localization: CustomerCenterConfigData.Localization,
    textColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CustomerCenterConstants.Card.CARD_PADDING,
                    vertical = 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(CustomerCenterConstants.Layout.ITEMS_SPACING),
        ) {
            Text(
                text = localization.commonLocalizedString(
                    CustomerCenterConfigData.Localization.CommonLocalizedString.DATE_WHEN_APP_WAS_PURCHASED,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = secondaryColor,
            )

            Text(
                text = originalPurchaseDate,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
        }
    }
}
