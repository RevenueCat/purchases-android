package com.revenuecat.sample.utils

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType

val Package.buttonText: String
    get() {
        with(product) {
            return if (type == ProductType.SUBS) {
                "${price.formatted} / ${period?.unit?.name?.lowercase()}"
            } else {
                "${price.formatted} one time"
            }
        }
    }
