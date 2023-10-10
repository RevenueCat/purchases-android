package com.revenuecat.purchases.ui.revenuecatui.fonts

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

@Composable
internal fun PaywallTheme(
    fontProvider: FontProvider?,
    content: @Composable () -> Unit,
) {
    if (fontProvider == null) {
        content()
    } else {
        val oldTypography = MaterialTheme.typography
        val typography = oldTypography.copy(
            displayLarge = oldTypography.displayLarge.modifyFontIfNeeded(TypographyType.DISPLAY_LARGE, fontProvider),
            displayMedium = oldTypography.displayMedium.modifyFontIfNeeded(TypographyType.DISPLAY_MEDIUM, fontProvider),
            displaySmall = oldTypography.displaySmall.modifyFontIfNeeded(TypographyType.DISPLAY_SMALL, fontProvider),
            headlineLarge = oldTypography.headlineLarge.modifyFontIfNeeded(TypographyType.HEADLINE_LARGE, fontProvider),
            headlineMedium = oldTypography.headlineMedium.modifyFontIfNeeded(
                TypographyType.HEADLINE_MEDIUM,
                fontProvider,
            ),
            headlineSmall = oldTypography.headlineSmall.modifyFontIfNeeded(TypographyType.HEADLINE_SMALL, fontProvider),
            titleLarge = oldTypography.titleLarge.modifyFontIfNeeded(TypographyType.TITLE_LARGE, fontProvider),
            titleMedium = oldTypography.titleMedium.modifyFontIfNeeded(TypographyType.TITLE_MEDIUM, fontProvider),
            titleSmall = oldTypography.titleSmall.modifyFontIfNeeded(TypographyType.TITLE_SMALL, fontProvider),
            bodyLarge = oldTypography.bodyLarge.modifyFontIfNeeded(TypographyType.BODY_LARGE, fontProvider),
            bodyMedium = oldTypography.bodyMedium.modifyFontIfNeeded(TypographyType.BODY_MEDIUM, fontProvider),
            bodySmall = oldTypography.bodySmall.modifyFontIfNeeded(TypographyType.BODY_SMALL, fontProvider),
            labelLarge = oldTypography.labelLarge.modifyFontIfNeeded(TypographyType.LABEL_LARGE, fontProvider),
            labelMedium = oldTypography.labelMedium.modifyFontIfNeeded(TypographyType.LABEL_MEDIUM, fontProvider),
            labelSmall = oldTypography.labelSmall.modifyFontIfNeeded(TypographyType.LABEL_SMALL, fontProvider),
        )
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = typography,
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}

private fun TextStyle.modifyFontIfNeeded(typographyType: TypographyType, fontProvider: FontProvider?): TextStyle {
    return if (fontProvider == null) {
        this
    } else {
        val font = fontProvider.getFont(typographyType)
        if (font == null) {
            this
        } else {
            this.copy(fontFamily = font)
        }
    }
}
