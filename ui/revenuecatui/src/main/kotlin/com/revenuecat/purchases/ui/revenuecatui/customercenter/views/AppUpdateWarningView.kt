package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData.Localization.CommonLocalizedString
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableIconSize
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingHorizontal
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingText
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ContentUnavailableViewPaddingVertical
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterUIConstants.ManagementViewHorizontalPadding
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButton
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonConfig
import com.revenuecat.purchases.ui.revenuecatui.customercenter.composables.SettingsButtonStyle
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme
import com.revenuecat.purchases.ui.revenuecatui.icons.Info

private val ButtonSpacing = 12.dp

/**
 * Shown instead of the management list when the customer is on an app version older than the one
 * published in the dashboard. Mirrors iOS `AppUpdateWarningView`.
 *
 * Not wired up yet, see PW-1361.
 */
@Composable
internal fun AppUpdateWarningView(
    localization: CustomerCenterConfigData.Localization,
    onUpdateAppClick: () -> Unit,
    onContinueAnywayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ManagementViewHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = ContentUnavailableViewPaddingVertical,
                        horizontal = ContentUnavailableViewPaddingHorizontal,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(ContentUnavailableIconSize),
                )

                Text(
                    text = localization.commonLocalizedString(CommonLocalizedString.UPDATE_WARNING_TITLE),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = ContentUnavailableViewPaddingText),
                )

                Text(
                    text = localization.commonLocalizedString(CommonLocalizedString.UPDATE_WARNING_DESCRIPTION),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = ContentUnavailableViewPaddingText),
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(ButtonSpacing)) {
            SettingsButton(
                title = localization.commonLocalizedString(CommonLocalizedString.UPDATE_WARNING_UPDATE),
                onClick = onUpdateAppClick,
                config = SettingsButtonConfig(),
                style = SettingsButtonStyle.FILLED,
            )

            SettingsButton(
                title = localization.commonLocalizedString(CommonLocalizedString.UPDATE_WARNING_IGNORE),
                onClick = onContinueAnywayClick,
                config = SettingsButtonConfig(),
                style = SettingsButtonStyle.OUTLINED,
            )
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun AppUpdateWarningView_Preview() {
    CustomerCenterPreviewTheme {
        AppUpdateWarningView(
            localization = CustomerCenterConfigTestData.customerCenterData().localization,
            onUpdateAppClick = {},
            onContinueAnywayClick = {},
        )
    }
}
