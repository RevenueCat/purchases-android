package com.revenuecat.purchases.common

import dev.drewhamilton.poko.Poko

@Poko
class PlatformInfo(
    val flavor: String,
    val version: String?,
)
