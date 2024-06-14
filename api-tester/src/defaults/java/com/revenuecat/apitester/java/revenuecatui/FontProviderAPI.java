package com.revenuecat.apitester.java.revenuecatui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.compose.ui.text.font.FontFamily;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.googlefonts.GoogleFont;
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomFontProvider;
import com.revenuecat.purchases.ui.revenuecatui.fonts.CustomParcelizableFontProvider;
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontProvider;
import com.revenuecat.purchases.ui.revenuecatui.fonts.GoogleFontProvider;
import com.revenuecat.purchases.ui.revenuecatui.fonts.ParcelizableFontProvider;
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFont;
import com.revenuecat.purchases.ui.revenuecatui.fonts.PaywallFontFamily;
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType;

import java.util.List;

@SuppressWarnings({"unused"})
final class FontProviderAPI {
    static void check(TypographyType typographyType) {
        FontProvider fontProvider = new FontProvider() {
            @Nullable
            @Override
            public FontFamily getFont(@NonNull TypographyType type) {
                return null;
            }
        };
        FontFamily fontFamily = fontProvider.getFont(typographyType);
        CustomFontProvider customFontProvider = new CustomFontProvider(FontFamily.Companion.getDefault());
    }

    static void checkParcelizableFontProvider(TypographyType typographyType, List<PaywallFont> fonts) {
        ParcelizableFontProvider fontProvider = new ParcelizableFontProvider() {
            @Nullable
            @Override
            public PaywallFontFamily getFont(@NonNull TypographyType type) {
                return null;
            }
        };
        PaywallFontFamily fontFamily = fontProvider.getFont(typographyType);
        CustomParcelizableFontProvider customFontProvider = new CustomParcelizableFontProvider(new PaywallFontFamily(fonts));
    }

    static void checkPaywallFontFamily(PaywallFontFamily fontFamily) {
        List<PaywallFont> fonts = fontFamily.getFonts();
    }

    static void checkPaywallFont(PaywallFont font) {
        if (font instanceof PaywallFont.GoogleFont) {
            final PaywallFont.GoogleFont googleFont = (PaywallFont.GoogleFont) font;
            String fontName = googleFont.getFontName();
            GoogleFontProvider provider = googleFont.getFontProvider();
            FontWeight fontWeight = googleFont.getFontWeight();
            int fontStyle = googleFont.getFontStyle();
            final PaywallFont.GoogleFont googleFont2 = new PaywallFont.GoogleFont(
                    fontName,
                    provider,
                    fontWeight,
                    fontStyle
            );
        } else if (font instanceof PaywallFont.ResourceFont) {
            final PaywallFont.ResourceFont resourceFont = (PaywallFont.ResourceFont) font;
            int fontRes = resourceFont.getResourceId();
            FontWeight fontWeight = resourceFont.getFontWeight();
            int fontStyle = resourceFont.getFontStyle();
            final PaywallFont.ResourceFont resourceFont2 = new PaywallFont.ResourceFont(fontRes, fontWeight, fontStyle);
        }
    }

    static void checkGoogleFontProvider(
            int certificates,
            String providerAuthority,
            String providerPackage
    ) {
        GoogleFontProvider provider = new GoogleFontProvider(certificates, providerAuthority, providerPackage);
        int providerCertificates = provider.getCertificates();
        String providerAuthority2 = provider.getProviderAuthority();
        String providerPackage2 = provider.getProviderPackage();
        GoogleFont.Provider googleProvider = provider.toGoogleProvider();
    }

    static Boolean checkTypographyType(TypographyType type) {
        switch (type) {
            case DISPLAY_LARGE:
            case DISPLAY_MEDIUM:
            case DISPLAY_SMALL:
            case HEADLINE_LARGE:
            case HEADLINE_MEDIUM:
            case HEADLINE_SMALL:
            case TITLE_LARGE:
            case TITLE_MEDIUM:
            case TITLE_SMALL:
            case BODY_LARGE:
            case BODY_MEDIUM:
            case BODY_SMALL:
            case LABEL_LARGE:
            case LABEL_MEDIUM:
            case LABEL_SMALL:
                return true;
        }
        return false;
    }
}
