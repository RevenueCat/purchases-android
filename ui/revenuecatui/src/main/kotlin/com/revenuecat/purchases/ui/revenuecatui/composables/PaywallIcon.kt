package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.R
import kotlin.random.Random

@Composable
internal fun PaywallIcon(
    icon: PaywallIconName,
    modifier: Modifier = Modifier,
    tintColor: Color,
) {
    Icon(
        modifier = Modifier
            .aspectRatio(1.0f)
            .fillMaxSize()
            .then(modifier),
        painter = painterResource(id = icon.drawable()),
        contentDescription = null,
        tint = tintColor,
    )
}

/**
 * An icon that can be displayed inside a RevenueCat paywall.
 */
internal enum class PaywallIconName {
    ADD,
    ANDROID,
    APPLE,
    ATTACH_MONEY,
    ATTACHMENT,
    BAR_CHART,
    BOOKMARK,
    BOOKMARK_NO_FILL,
    CALENDAR_TODAY,
    CHAT_BUBBLE,
    CHECK_CIRCLE,
    CLOSE,
    COLLAPSE,
    COMPARE,
    DOWNLOAD,
    EDIT,
    EMAIL,
    ERROR,
    EXPERIMENTS,
    EXTENSION,
    FILE_COPY,
    FILTER_LIST,
    FOLDER,
    GLOBE,
    HELP,
    INSERT_DRIVE_FILE,
    LAUNCH,
    LAYERS,
    LINE_CHART,
    LOCK,
    NOTIFICATION,
    PERSON,
    PHONE,
    PLAY_CIRCLE,
    REMOVE_RED_EYE,
    SEARCH,
    SHARE,
    SMARTPHONE,
    STACKED_BAR,
    STARS,
    SUBTRACT,
    TICK,
    TRANSFER,
    TWO_WAY_ARROWS,
    KEY,
    WARNING,
    ;

    companion object {
        private val valueMap = values().associateBy { it.name.lowercase() }

        fun fromValue(value: String): PaywallIconName? {
            return valueMap[value]
        }
    }

    @Suppress("CyclomaticComplexMethod")
    internal fun drawable(): Int {
        return when (this) {
            WARNING -> R.drawable.warning
            ADD -> R.drawable.add
            ANDROID -> R.drawable.android
            APPLE -> R.drawable.apple
            ATTACH_MONEY -> R.drawable.attach_money
            ATTACHMENT -> R.drawable.attachment
            BAR_CHART -> R.drawable.bar_chart
            BOOKMARK -> R.drawable.bookmark
            BOOKMARK_NO_FILL -> R.drawable.bookmark_no_fill
            CALENDAR_TODAY -> R.drawable.calendar_today
            CHAT_BUBBLE -> R.drawable.chat_bubble
            CHECK_CIRCLE -> R.drawable.check_circle
            CLOSE -> R.drawable.close
            COLLAPSE -> R.drawable.collapse
            COMPARE -> R.drawable.compare
            DOWNLOAD -> R.drawable.download
            EDIT -> R.drawable.edit
            EMAIL -> R.drawable.email
            ERROR -> R.drawable.error
            EXPERIMENTS -> R.drawable.experiments
            EXTENSION -> R.drawable.extension
            FILE_COPY -> R.drawable.file_copy
            FILTER_LIST -> R.drawable.filter_list
            FOLDER -> R.drawable.folder
            GLOBE -> R.drawable.globe
            HELP -> R.drawable.help
            INSERT_DRIVE_FILE -> R.drawable.insert_drive_file
            LAUNCH -> R.drawable.launch
            LAYERS -> R.drawable.layers
            LINE_CHART -> R.drawable.line_chart
            LOCK -> R.drawable.lock
            NOTIFICATION -> R.drawable.notifications
            PERSON -> R.drawable.person
            PHONE -> R.drawable.phone
            PLAY_CIRCLE -> R.drawable.play_circle
            REMOVE_RED_EYE -> R.drawable.remove_red_eye
            SEARCH -> R.drawable.search
            SHARE -> R.drawable.share
            SMARTPHONE -> R.drawable.smartphone
            STACKED_BAR -> R.drawable.stacked_bar
            STARS -> R.drawable.stars
            SUBTRACT -> R.drawable.subtract
            TICK -> R.drawable.tick
            TRANSFER -> R.drawable.transfer
            TWO_WAY_ARROWS -> R.drawable.two_way_arrows
            KEY -> R.drawable.vpn_key
        }
    }
}

@Preview(showBackground = true, widthDp = 300)
@Composable
internal fun PaywallIconPreview() {
    val icons = PaywallIconName.values()

    @Suppress("MagicNumber")
    fun randomColor(): Color {
        return Color(
            red = Random.nextInt(0, 256),
            green = Random.nextInt(0, 256),
            blue = Random.nextInt(0, 256),
        )
    }

    LazyVerticalGrid(columns = GridCells.Adaptive(40.dp)) {
        items(icons.size) {
            Box(modifier = Modifier.background(randomColor())) {
                PaywallIcon(icon = icons[it], tintColor = Color.Black)
            }
        }
    }
}
