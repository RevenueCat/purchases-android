package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.compose.ui.unit.dp

internal object CustomerCenterConstants {
    object Card {
        val ROUNDED_CORNER_SIZE = 24.dp
        val MIDDLE_CORNER_SIZE = 4.dp
        val BADGE_CORNER_SIZE = 4.dp
        val CARD_PADDING = 24.dp
        val TITLE_ROW_BOTTOM_PADDING = 4.dp
        val BADGE_HORIZONTAL_PADDING = 8.dp
        val BADGE_VERTICAL_PADDING = 2.dp
        const val COLOR_BADGE_CANCELLED = 0x33F2545B
        const val COLOR_BADGE_FREE_TRIAL = 0x5BF5CA5C
        const val COLOR_BADGE_ACTIVE = 0x9911D483
        const val COLOR_BADGE_EXPIRED = 0x1A1D1B20
        const val LIFETIME_BORDER_ALPHA = 0.29f
    }

    object Layout {
        val TOP_PADDING_AFTER_TOP_BAR = 8.dp
        val HORIZONTAL_PADDING = 16.dp
        val ITEMS_SPACING = 2.dp
        val SECTION_SPACING = 24.dp
        val SECTION_TITLE_BOTTOM_PADDING = 8.dp
        val BUTTONS_TOP_PADDING = 24.dp
        val BUTTONS_BOTTOM_PADDING = 24.dp
        val BUTTONS_SPACING = 12.dp
    }

    object Management {
        const val MANAGEMENT_URL = "https://play.google.com/store/account/subscriptions"
    }
}
