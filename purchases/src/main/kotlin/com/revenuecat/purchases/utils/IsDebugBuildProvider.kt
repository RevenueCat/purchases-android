package com.revenuecat.purchases.utils

import android.content.Context
import android.content.pm.ApplicationInfo

internal fun interface IsDebugBuildProvider {
    operator fun invoke(): Boolean
}

internal class DefaultIsDebugBuildProvider(context: Context) : IsDebugBuildProvider {
    private val context: Context = context.applicationContext

    override fun invoke(): Boolean =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
