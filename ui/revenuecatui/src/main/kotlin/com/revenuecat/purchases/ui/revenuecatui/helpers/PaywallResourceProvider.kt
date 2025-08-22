package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.ui.text.font.FontFamily
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.DownloadedFontFamily
import com.revenuecat.purchases.ui.revenuecatui.utils.FontFamilyXmlParser
import java.util.Locale

/**
 * Abstraction around [Context]
 */
internal interface ResourceProvider {
    companion object {
        const val ASSETS_FONTS_DIR = "fonts"
    }

    fun getApplicationName(): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
    fun getLocale(): Locale
    fun getResourceIdentifier(name: String, type: String): Int
    fun getXmlFontFamily(resourceId: Int): FontFamily?
    fun getAssetFontPath(name: String): String?
    fun getCachedFontFamilyOrStartDownload(
        fontInfo: UiConfig.AppConfig.FontsConfig.FontInfo.Name,
    ): DownloadedFontFamily?
}

internal class PaywallResourceProvider(
    private val applicationName: String,
    private val packageName: String,
    private val resources: Resources,
) : ResourceProvider {
    constructor(
        context: Context,
    ) : this(context.applicationContext.applicationName(), context.packageName, context.resources)

    override fun getApplicationName(): String {
        return applicationName
    }

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return resources.getString(resId, *formatArgs)
    }

    override fun getLocale(): Locale {
        return resources.configuration.locales.get(0)
    }

    /**
     * Use sparingly. The underlying platform API is discouraged because
     * > resource reflection makes it harder to perform build optimizations and compile-time verification of code.
     */
    @SuppressLint("DiscouragedApi")
    override fun getResourceIdentifier(name: String, type: String): Int =
        resources.getIdentifier(name, type, packageName)

    @Suppress("ReturnCount")
    override fun getXmlFontFamily(resourceId: Int): FontFamily? {
        val parser = try {
            resources.getXml(resourceId)
        } catch (_: Resources.NotFoundException) {
            return null
        }
        return try {
            FontFamilyXmlParser.parse(parser)
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            // This can happen if the XML is malformed or not a valid font family.
            // We log the error and return null.
            val resourceName = resources.getResourceEntryNameOrNull(resourceId)
            Logger.e("Error parsing XML font family with resource ID ${resourceName ?: resourceId}", e)
            null
        } finally {
            parser.close()
        }
    }

    override fun getAssetFontPath(name: String): String? {
        val nameWithExtension = if (name.endsWith(".ttf")) name else "$name.ttf"

        return resources.assets.list(ResourceProvider.ASSETS_FONTS_DIR)
            ?.find { it == nameWithExtension }
            ?.let { "${ResourceProvider.ASSETS_FONTS_DIR}/$it" }
    }

    override fun getCachedFontFamilyOrStartDownload(
        fontInfo: UiConfig.AppConfig.FontsConfig.FontInfo.Name,
    ): DownloadedFontFamily? {
        return if (Purchases.isConfigured) {
            Purchases.sharedInstance.getCachedFontFamilyOrStartDownload(fontInfo)
        } else {
            Logger.e("getCachedFontFileOrStartDownload called before Purchases is configured. Returning null.")
            null
        }
    }
}

internal fun Context.toResourceProvider(): ResourceProvider {
    return PaywallResourceProvider(this)
}

private fun Context.applicationName(): String {
    return applicationInfo.loadLabel(packageManager).toString()
}

private fun Resources.getResourceEntryNameOrNull(resourceId: Int): String? =
    try {
        getResourceEntryName(resourceId)
    } catch (_: Resources.NotFoundException) {
        null
    }
