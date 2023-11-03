package com.revenuecat.apitester.kotlin.revenuecatui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontFamily.Companion.Default
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.GoogleFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFont
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFontFamily
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType

@Suppress("unused", "UNUSED_VARIABLE")
@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
private class FontProviderAPI {
    fun check(typographyType: TypographyType) {
        val fontProvider: FontProvider = object : FontProvider {
            override fun getFont(type: TypographyType): FontFamily? {
                return null
            }
        }
        val fontFamily = fontProvider.getFont(typographyType)
        val customFontProvider = CustomFontProvider(Default)
    }

    fun checkParcelizableFontProvider(typographyType: TypographyType, fonts: List<PaywallFont>) {
        val fontProvider: ParcelizableFontProvider = object : ParcelizableFontProvider {
            override fun getFont(type: TypographyType): PaywallFontFamily? {
                return null
            }
        }
        val fontFamily = fontProvider.getFont(typographyType)
        val customFontProvider = CustomParcelizableFontProvider(PaywallFontFamily(fonts))
    }

    fun checkPaywallFontFamily(fontFamily: PaywallFontFamily) {
        val fonts = fontFamily.fonts
    }

    fun checkPaywallFont(font: PaywallFont) {
        if (font is PaywallFont.GoogleFont) {
            val fontName: String = font.fontName
            val provider: GoogleFontProvider = font.fontProvider
            val fontWeight: FontWeight = font.fontWeight
            val fontStyle: Int = font.fontStyle
            val googleFont = PaywallFont.GoogleFont(
                fontName,
                provider,
                fontWeight,
                fontStyle,
            )
        } else if (font is PaywallFont.ResourceFont) {
            val fontRes: Int = font.resourceId
            val fontWeight: FontWeight = font.fontWeight
            val fontStyle: Int = font.fontStyle
            val resourceFont = PaywallFont.ResourceFont(fontRes, fontWeight, fontStyle)
        }
    }

    fun checkGoogleFontProvider(
        certificates: Int,
        providerAuthority: String?,
        providerPackage: String?,
    ) {
        val provider = GoogleFontProvider(certificates, providerAuthority!!, providerPackage!!)
        val providerCertificates = provider.certificates
        val providerAuthority2 = provider.providerAuthority
        val providerPackage2 = provider.providerPackage
        val googleProvider: GoogleFont.Provider = provider.toGoogleProvider()
    }

    fun checkTypographyType(type: TypographyType): Boolean {
        when (type) {
            TypographyType.DISPLAY_LARGE,
            TypographyType.DISPLAY_MEDIUM,
            TypographyType.DISPLAY_SMALL,
            TypographyType.HEADLINE_LARGE,
            TypographyType.HEADLINE_MEDIUM,
            TypographyType.HEADLINE_SMALL,
            TypographyType.TITLE_LARGE,
            TypographyType.TITLE_MEDIUM,
            TypographyType.TITLE_SMALL,
            TypographyType.BODY_LARGE,
            TypographyType.BODY_MEDIUM,
            TypographyType.BODY_SMALL,
            TypographyType.LABEL_LARGE,
            TypographyType.LABEL_MEDIUM,
            TypographyType.LABEL_SMALL,
            -> return true
        }
    }
}
