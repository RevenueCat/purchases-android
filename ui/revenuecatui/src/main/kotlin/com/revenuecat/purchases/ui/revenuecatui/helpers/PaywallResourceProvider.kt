package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import com.revenuecat.purchases.Purchases
import java.io.File
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
    fun getAssetFontPath(name: String): String?
    fun getCachedFontFileOrStartDownload(url: String, expectedMd5: String): File?
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

    override fun getAssetFontPath(name: String): String? {
        val nameWithExtension = if (name.endsWith(".ttf")) name else "$name.ttf"

        return resources.assets.list(ResourceProvider.ASSETS_FONTS_DIR)
            ?.find { it == nameWithExtension }
            ?.let { "${ResourceProvider.ASSETS_FONTS_DIR}/$it" }
    }

    override fun getCachedFontFileOrStartDownload(url: String, expectedMd5: String): File? {
        return if (Purchases.isConfigured) {
            Purchases.sharedInstance.getCachedFontFileOrStartDownload(url, expectedMd5)
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
