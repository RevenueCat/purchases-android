package com.revenuecat.purchases.common

import java.lang.reflect.Method

internal object ThreadAllocationMeter {
    private val managementFactoryGetThreadMXBean: Method =
        Class.forName("java.lang.management.ManagementFactory").getMethod("getThreadMXBean")
    private val getThreadAllocatedBytesMethod: Method =
        Class.forName("com.sun.management.ThreadMXBean")
            .getMethod("getThreadAllocatedBytes", Long::class.javaPrimitiveType)

    fun measure(block: () -> Unit): Long {
        val threadId = Thread.currentThread().id
        val before = allocatedBytes(threadId)
        check(before >= 0) { "Thread allocation tracking is unavailable." }
        block()
        return allocatedBytes(threadId) - before
    }

    private fun allocatedBytes(threadId: Long): Long {
        val bean = managementFactoryGetThreadMXBean.invoke(null)
        return getThreadAllocatedBytesMethod.invoke(bean, threadId) as Long
    }
}
