package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.composables.Fade
import com.revenuecat.purchases.ui.revenuecatui.composables.PlaceholderDefaults
import com.revenuecat.purchases.ui.revenuecatui.composables.placeholder
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterConstants
import com.revenuecat.purchases.ui.revenuecatui.customercenter.theme.CustomerCenterPreviewTheme

private object LoadingViewConstants {
    const val PLACEHOLDER_ALPHA = 0.15f
    val PLACEHOLDER_SHAPE = RoundedCornerShape(4.dp)
    val TITLE_WIDTH = 180.dp
    val TITLE_HEIGHT = 20.dp
    val SUBTITLE_WIDTH = 220.dp
    val BODY_HEIGHT = 16.dp
    val STORE_WIDTH = 100.dp
    val BUTTON_HEIGHT = 48.dp
    val BUTTON_SHAPE = RoundedCornerShape(24.dp)
    val BADGE_WIDTH = 60.dp
    val BADGE_HEIGHT = 22.dp
    val BADGE_SHAPE = RoundedCornerShape(CustomerCenterConstants.Card.BADGE_CORNER_SIZE)
}

@Composable
internal fun CustomerCenterLoadingView() {
    val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = LoadingViewConstants.PLACEHOLDER_ALPHA)
    val fadeHighlight = Fade(
        highlightColor = placeholderColor,
        animationSpec = PlaceholderDefaults.fadeAnimationSpec,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CustomerCenterConstants.Layout.HORIZONTAL_PADDING),
        horizontalAlignment = Alignment.Start,
    ) {
        // Skeleton card mimicking PurchaseInformationCardView
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CustomerCenterConstants.Card.ROUNDED_CORNER_SIZE),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        ) {
            Column(
                modifier = Modifier.padding(CustomerCenterConstants.Card.CARD_PADDING),
            ) {
                // Title row with badge
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = CustomerCenterConstants.Card.TITLE_ROW_BOTTOM_PADDING),
                ) {
                    Box(
                        modifier = Modifier
                            .width(LoadingViewConstants.TITLE_WIDTH)
                            .height(LoadingViewConstants.TITLE_HEIGHT)
                            .placeholder(
                                visible = true,
                                color = placeholderColor,
                                shape = LoadingViewConstants.PLACEHOLDER_SHAPE,
                                highlight = fadeHighlight,
                            ),
                    )
                    Box(
                        modifier = Modifier
                            .width(LoadingViewConstants.BADGE_WIDTH)
                            .height(LoadingViewConstants.BADGE_HEIGHT)
                            .placeholder(
                                visible = true,
                                color = placeholderColor,
                                shape = LoadingViewConstants.BADGE_SHAPE,
                                highlight = fadeHighlight,
                            ),
                    )
                }
                // Subtitle line
                Box(
                    modifier = Modifier
                        .width(LoadingViewConstants.SUBTITLE_WIDTH)
                        .height(LoadingViewConstants.BODY_HEIGHT)
                        .placeholder(
                            visible = true,
                            color = placeholderColor,
                            shape = LoadingViewConstants.PLACEHOLDER_SHAPE,
                            highlight = fadeHighlight,
                        ),
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Store text line
                Box(
                    modifier = Modifier
                        .width(LoadingViewConstants.STORE_WIDTH)
                        .height(LoadingViewConstants.BODY_HEIGHT)
                        .placeholder(
                            visible = true,
                            color = placeholderColor,
                            shape = LoadingViewConstants.PLACEHOLDER_SHAPE,
                            highlight = fadeHighlight,
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.height(CustomerCenterConstants.Layout.BUTTONS_TOP_PADDING))

        // Skeleton buttons mimicking ManageSubscriptionsButtonsView
        Column(
            verticalArrangement = Arrangement.spacedBy(CustomerCenterConstants.Layout.BUTTONS_SPACING),
        ) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = LoadingViewConstants.BUTTON_HEIGHT)
                        .placeholder(
                            visible = true,
                            color = placeholderColor,
                            shape = LoadingViewConstants.BUTTON_SHAPE,
                            highlight = fadeHighlight,
                        ),
                )
            }
        }
    }
}

@Preview(
    name = "Customer Center Loading View",
    showBackground = true,
)
@Composable
internal fun CustomerCenterLoadingViewPreview() {
    CustomerCenterPreviewTheme {
        CustomerCenterLoadingView()
    }
}
