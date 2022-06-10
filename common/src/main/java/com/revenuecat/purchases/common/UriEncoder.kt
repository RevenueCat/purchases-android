package com.revenuecat.purchases.common

import android.net.Uri

class UriEncoder {
    fun encode(string: String): String {
        return Uri.encode(string)
    }
}
