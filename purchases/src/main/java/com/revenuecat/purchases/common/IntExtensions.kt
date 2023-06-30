package com.revenuecat.purchases.common

import java.nio.ByteBuffer
import java.nio.ByteOrder

fun Int.Companion.fromLittleEndianBytes(byteArray: ByteArray): Int {
    return ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).int
}
