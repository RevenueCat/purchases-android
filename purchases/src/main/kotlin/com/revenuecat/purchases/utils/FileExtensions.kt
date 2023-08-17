package com.revenuecat.purchases.utils

import java.io.File

private const val BYTE_UNIT_CONVERSION: Double = 1024.0

val File.sizeInBytes: Long
    get() = length()
val File.sizeInKB: Double
    get() = sizeInBytes / BYTE_UNIT_CONVERSION
