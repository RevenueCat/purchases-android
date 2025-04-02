package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import androidx.annotation.StringRes
import java.util.Locale

/**
 * Abstraction around [Context]
 */
internal interface ResourceProvider {
    companion object {
        const val DEFAULT_ASSETS_FONTS_DIR = "fonts"
    }

    fun getApplicationName(): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
    fun getLocale(): Locale
    fun getResourceIdentifier(name: String, type: String): Int
    fun getAssetFontPath(name: String): String?
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
        // We'll first check the default "assets/fonts" folder, and fall back to traversing all of "assets/".
        return resources.assets.list(ResourceProvider.DEFAULT_ASSETS_FONTS_DIR)
            ?.find { it == nameWithExtension }
            ?.let { "${ResourceProvider.DEFAULT_ASSETS_FONTS_DIR}/$it" }
            ?: resources.assets.firstOrNull { it == nameWithExtension }
    }

    /**
     * Returns the first asset path that satisfies [predicate].
     */
    private inline fun AssetManager.firstOrNull(predicate: (path: String) -> Boolean): String? {
        asSequence().forEach { element -> if (predicate(element)) return element }
        return null
    }

    /**
     * Traverses all assets and yields each one to the returned Sequence.
     */
    private fun AssetManager.asSequence(path: String = ""): Sequence<String> = sequence {
        val children = list(path) ?: emptyArray()
        if (children.isEmpty()) {
            // This is a leaf node.
            if (path.isNotEmpty()) yield(path)
        } else {
            for (child in children) {
                val fullPath = if (path.isEmpty()) child else "$path/$child"
                yieldAll(asSequence(fullPath))
            }
        }
    }
}

internal fun Context.toResourceProvider(): ResourceProvider {
    return PaywallResourceProvider(this)
}

private fun Context.applicationName(): String {
    return applicationInfo.loadLabel(packageManager).toString()
}
