package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context

/**
 * Abstraction around [Context]
 */
internal interface ApplicationContext {
    fun getApplicationName(): String
}

internal class AndroidApplicationContext(private val applicationContext: Context) : ApplicationContext {
    override fun getApplicationName(): String {
        return applicationContext.applicationInfo.loadLabel(applicationContext.packageManager).toString()
    }
}

internal fun Context.toAndroidContext(): ApplicationContext {
    return AndroidApplicationContext(applicationContext)
}
