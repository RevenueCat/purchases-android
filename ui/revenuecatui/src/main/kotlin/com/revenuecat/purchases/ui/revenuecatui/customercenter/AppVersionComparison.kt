package com.revenuecat.purchases.ui.revenuecatui.customercenter

private const val VERSION_SEPARATOR = "."
private const val PRERELEASE_SEPARATOR = "-"
private const val BUILD_SEPARATOR = "+"

/**
 * Whether [currentVersion] is older than [latestVersion].
 *
 * The core SDK's `rc.semverCompare` lives in the purchases module and is internal, so this is a
 * deliberately small local comparison: numeric core components only, a missing component counts
 * as 0 ("2.1" is "2.1.0"), and prerelease/build suffixes are dropped rather than ordered.
 *
 * Returns false whenever either side is missing or doesn't parse, so an unrecognised version
 * string never nags the customer.
 */
internal fun isAppVersionOutdated(currentVersion: String?, latestVersion: String?): Boolean =
    compareVersionCores(currentVersion, latestVersion)?.let { it < 0 } ?: false

private fun compareVersionCores(left: String?, right: String?): Int? {
    val leftCore = parseVersionCore(left)
    val rightCore = parseVersionCore(right)
    return if (leftCore == null || rightCore == null) {
        null
    } else {
        val componentCount = maxOf(leftCore.size, rightCore.size)
        (0 until componentCount)
            .map { leftCore.getOrElse(it) { 0L }.compareTo(rightCore.getOrElse(it) { 0L }) }
            .firstOrNull { it != 0 } ?: 0
    }
}

private fun parseVersionCore(version: String?): List<Long>? {
    val core = version
        ?.trim()
        ?.substringBefore(BUILD_SEPARATOR)
        ?.substringBefore(PRERELEASE_SEPARATOR)

    return if (core.isNullOrEmpty()) {
        null
    } else {
        val components = core.split(VERSION_SEPARATOR).map { it.toLongOrNull() }
        if (components.any { it == null }) null else components.filterNotNull()
    }
}

/**
 * The host app's version name. `AppConfig.versionName` in the core SDK is internal to that module,
 * so this reads it directly. Null when the package manager can't tell us.
 */
internal fun android.content.Context.currentAppVersion(): String? = runCatching {
    packageManager.getPackageInfo(packageName, 0).versionName
}.getOrNull()
