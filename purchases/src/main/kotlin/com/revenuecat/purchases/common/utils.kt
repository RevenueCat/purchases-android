//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Base64
import java.security.MessageDigest
import java.util.Locale

const val MICROS_MULTIPLIER = 1_000_000

internal fun Context.getLocale(): Locale? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        resources.configuration.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        resources.configuration.locale
    }

internal fun Locale.toBCP47(): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return toLanguageTag()
    }

    // we will use a dash as per BCP 47
    val separator = '-'
    var language = language
    var region = country
    var variant = variant

    // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
    // this goes before the string matching since "NY" wont pass the variant checks
    if (language == "no" && region == "NO" && variant == "NY") {
        language = "nn"
        region = "NO"
        variant = ""
    }

    if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}".toRegex())) {
        language = "und" // Follow the Locale#toLanguageTag() implementation
        // which says to return "und" for Undetermined
    } else if (language == "iw") {
        language = "he" // correct deprecated "Hebrew"
    } else if (language == "in") {
        language = "id" // correct deprecated "Indonesian"
    } else if (language == "ji") {
        language = "yi" // correct deprecated "Yiddish"
    }

    // ensure valid country code, if not well formed, it's omitted
    if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}".toRegex())) {
        region = ""
    }

    // variant subtags that begin with a letter must be at least 5 characters long
    if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}".toRegex())) {
        variant = ""
    }

    val bcp47Tag = StringBuilder(language)
    if (region.isNotEmpty()) {
        bcp47Tag.append(separator).append(region)
    }
    if (variant.isNotEmpty()) {
        bcp47Tag.append(separator).append(variant)
    }

    return bcp47Tag.toString()
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

val Context.playStorePackageInfo: PackageInfo?
    get() = try {
        this.packageManager.getPackageInfo("com.android.vending", 0)
    } catch (e: Throwable) {
        null
    }