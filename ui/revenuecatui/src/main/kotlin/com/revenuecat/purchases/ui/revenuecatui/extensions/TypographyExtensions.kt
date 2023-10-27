package com.revenuecat.purchases.ui.revenuecatui.extensions

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
fun Typography.copyWithFontProvider(fontProvider: FontProvider): Typography {
    with(this) {
        return copy(
            displayLarge = displayLarge.modifyFontIfNeeded(TypographyType.DISPLAY_LARGE, fontProvider),
            displayMedium = displayMedium.modifyFontIfNeeded(TypographyType.DISPLAY_MEDIUM, fontProvider),
            displaySmall = displaySmall.modifyFontIfNeeded(TypographyType.DISPLAY_SMALL, fontProvider),
            headlineLarge = headlineLarge.modifyFontIfNeeded(TypographyType.HEADLINE_LARGE, fontProvider),
            headlineMedium = headlineMedium.modifyFontIfNeeded(TypographyType.HEADLINE_MEDIUM, fontProvider),
            headlineSmall = headlineSmall.modifyFontIfNeeded(TypographyType.HEADLINE_SMALL, fontProvider),
            titleLarge = titleLarge.modifyFontIfNeeded(TypographyType.TITLE_LARGE, fontProvider),
            titleMedium = titleMedium.modifyFontIfNeeded(TypographyType.TITLE_MEDIUM, fontProvider),
            titleSmall = titleSmall.modifyFontIfNeeded(TypographyType.TITLE_SMALL, fontProvider),
            bodyLarge = bodyLarge.modifyFontIfNeeded(TypographyType.BODY_LARGE, fontProvider),
            bodyMedium = bodyMedium.modifyFontIfNeeded(TypographyType.BODY_MEDIUM, fontProvider),
            bodySmall = bodySmall.modifyFontIfNeeded(TypographyType.BODY_SMALL, fontProvider),
            labelLarge = labelLarge.modifyFontIfNeeded(TypographyType.LABEL_LARGE, fontProvider),
            labelMedium = labelMedium.modifyFontIfNeeded(TypographyType.LABEL_MEDIUM, fontProvider),
            labelSmall = labelSmall.modifyFontIfNeeded(TypographyType.LABEL_SMALL, fontProvider),
        )
    }
}

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
private fun TextStyle.modifyFontIfNeeded(typographyType: TypographyType, fontProvider: FontProvider): TextStyle {
    val font = fontProvider.getFont(typographyType)
    return if (font == null) {
        this
    } else {
        this.copy(fontFamily = font)
    }
}
