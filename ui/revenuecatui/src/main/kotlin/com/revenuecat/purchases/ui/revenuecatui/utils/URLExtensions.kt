@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.utils

import java.net.URL

@JvmSynthetic
internal fun URL.appendQueryParameter(name: String, value: String): URL =
    toURI().appendQueryParameter(name, value).toURL()
