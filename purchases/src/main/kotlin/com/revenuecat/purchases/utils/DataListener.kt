package com.revenuecat.purchases.utils

interface DataListener<T> {
    fun onData(data: T)
    fun onComplete()
}
