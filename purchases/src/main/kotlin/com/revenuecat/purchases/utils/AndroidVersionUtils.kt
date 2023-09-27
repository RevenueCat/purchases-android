package com.revenuecat.purchases.utils

import android.os.Build

internal fun isAndroidNOrNewer() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
