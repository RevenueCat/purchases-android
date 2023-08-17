package com.revenuecat.purchases.utils

import android.os.Build

fun isAndroidNOrNewer() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
