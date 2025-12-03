package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.Context

/**
 * Converts a version string to an integer by keeping only numeric characters.
 * For example: "3.2.0" -> 320, "1.0.0-beta1" -> 1001
 * Returns null if the resulting string is empty or cannot be parsed.
 */
internal fun String.toVersionInt(): Int? {
    val digitsOnly = this.filter { it.isDigit() }
    return digitsOnly.toIntOrNull()
}

/**
 * Gets the app's version name from the package info.
 */
internal val Context.versionName: String?
    get() = this.packageManager.getPackageInfo(this.packageName, 0).versionName

/**
 * Gets the app's version as an integer from the package info.
 * Converts the version name to an integer by keeping only numeric characters.
 */
internal val Context.versionInt: Int?
    get() = versionName?.toVersionInt()
