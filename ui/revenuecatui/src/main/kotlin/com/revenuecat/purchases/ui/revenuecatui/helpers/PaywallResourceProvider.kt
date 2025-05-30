package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.paywalls.components.properties.FontSpec
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
    fun getCachedFontSpecs(): Map<FontAlias, FontSpec>
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

    override fun getCachedFontSpecs(): Map<FontAlias, FontSpec> {
        return Purchases.sharedInstance.getCachedFontSpecs()
    }
}

internal fun Context.toResourceProvider(): ResourceProvider {
    return PaywallResourceProvider(this)
}

private fun Context.applicationName(): String {
    return applicationInfo.loadLabel(packageManager).toString()
}
