package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context
import androidx.annotation.StringRes

/**
 * Abstraction around [Context]
 */
internal interface ApplicationContext {
    fun getApplicationName(): String
    fun getString(@StringRes resId: Int): String
}

internal class AndroidApplicationContext(private val applicationContext: Context) : ApplicationContext {
    override fun getApplicationName(): String {
        return applicationContext.applicationInfo.loadLabel(applicationContext.packageManager).toString()
    }

    override fun getString(@StringRes resId: Int): String {
        return applicationContext.resources.getString(resId)
    }
}

internal fun Context.toAndroidContext(): ApplicationContext {
    return AndroidApplicationContext(applicationContext)
}
