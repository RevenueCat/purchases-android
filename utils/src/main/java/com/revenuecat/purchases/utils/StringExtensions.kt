package com.revenuecat.purchases.utils

val String.notEmpty: String?
    get() {
        return if (this.isBlank()) {
            null
        } else {
            this
        }
    }
