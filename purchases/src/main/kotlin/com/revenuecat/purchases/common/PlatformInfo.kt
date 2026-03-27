package com.revenuecat.purchases.common

import dev.drewhamilton.poko.Poko

@Poko
public class PlatformInfo(
    public val flavor: String,
    public val version: String?,
)
