//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import java.security.MessageDigest
import java.util.Locale

internal const val MICROS_MULTIPLIER = 1_000_000

internal fun Context.getLocale(): Locale? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        resources.configuration.locale
    }

internal fun String.sha1() =
    MessageDigest.getInstance("SHA-1")
        .digest(this.toByteArray()).let {
            String(Base64.encode(it, Base64.NO_WRAP))
        }

internal fun String.sha256() =
    MessageDigest.getInstance("SHA-256")
        .digest(this.toByteArray()).let {
            String(Base64.encode(it, Base64.NO_WRAP))
        }

internal val Context.versionName: String?
    get() = this.packageManager.getPackageInfo(this.packageName, 0).versionName

private fun Context.packageVersionName(packageName: String): String? {
    return try {
        this.packageManager.getPackageInfo(packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
internal val Context.playStoreVersionName: String?
    get() = packageVersionName("com.android.vending")

internal val Context.playServicesVersionName: String?
    get() = packageVersionName("com.google.android.gms")
