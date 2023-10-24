package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context
import androidx.annotation.StringRes
import java.util.Locale

/**
 * Abstraction around [Context]
 */
internal interface ApplicationContext {
    fun getApplicationName(): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String
    fun getLocale(): Locale
}

internal class AndroidApplicationContext(private val applicationContext: Context) : ApplicationContext {
    override fun getApplicationName(): String {
        return applicationContext.applicationInfo.loadLabel(applicationContext.packageManager).toString()
    }

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any?): String {
        return applicationContext.resources.getString(resId, formatArgs)
    }

    override fun getLocale(): Locale {
        return applicationContext.resources.configuration.locales.get(0)
    }
}

internal fun Context.toAndroidContext(): ApplicationContext {
    return AndroidApplicationContext(applicationContext)
}
