package com.revenuecat.purchases.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IsDebugBuildProviderTest {

    @Test
    fun `Correctly determines non-debug builds`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val isDebugBuild = DefaultIsDebugBuildProvider(context)
        val expected = false
        context.applicationInfo.setDebuggable(debuggable = expected)

        val actual = isDebugBuild()

        assert(actual == expected)
    }

    @Test
    fun `Correctly determines debug builds`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val isDebugBuild = DefaultIsDebugBuildProvider(context)
        val expected = true
        context.applicationInfo.setDebuggable(debuggable = expected)

        val actual = isDebugBuild()

        assert(actual == expected)
    }

    private fun ApplicationInfo.setDebuggable(debuggable: Boolean) {
        flags =
            if (debuggable) flags or ApplicationInfo.FLAG_DEBUGGABLE
            else flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
    }

}
