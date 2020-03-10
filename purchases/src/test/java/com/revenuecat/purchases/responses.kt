package com.revenuecat.purchases

import java.io.File

class Responses {
    companion object {
        val validFullPurchaserResponse by lazy {
            getJSONFromPath("responses/valid_full_purchaser_response.json")
        }
        val validEmptyPurchaserResponse by lazy {
            getJSONFromPath("responses/valid_empty_purchaser_response.json")
        }
        val subscriberAttributesErrorsPostReceiptResponse by lazy {
            getJSONFromPath("responses/attribute_errors_post_receipt_response.json")
        }

        private fun getJSONFromPath(fileName: String): String {
            val classLoader = this::class.java.classLoader
            val resource = classLoader!!.getResource(fileName)
            return File(resource.path).readText()
        }
    }
}
