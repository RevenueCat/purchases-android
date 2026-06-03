package com.revenuecat.purchases.flatbuffers

import android.util.Base64
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.flatbuffers.generated.ProductsSection
import org.json.JSONObject
import java.nio.ByteBuffer

/**
 * Proof of concept: parses a FlatBuffers-encoded "section" delivered inside a regular JSON
 * backend response.
 *
 * The backend embeds the binary FlatBuffer as a base64 string in a dedicated JSON field
 * ([PRODUCTS_SECTION_FIELD]). We read that one field from the already-parsed
 * `HTTPResult.body`, base64-decode it, and read the buffer with zero-copy generated accessors.
 *
 * Error handling intentionally matches `OfferingParser`: any failure is logged and yields
 * `null` so a malformed section can never crash response parsing.
 */
internal object FlatBuffersProductsSectionParser {

    private const val PRODUCTS_SECTION_FIELD = "products_section_fb"

    @Suppress("TooGenericExceptionCaught")
    fun parse(body: JSONObject): ProductsSectionData? {
        val base64 = body.optString(PRODUCTS_SECTION_FIELD).takeIf { it.isNotBlank() }
            ?: return null

        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val section = ProductsSection.getRootAsProductsSection(ByteBuffer.wrap(bytes))

            val products = (0 until section.productsLength).mapNotNull { index ->
                section.products(index)?.let { product ->
                    ProductData(
                        id = product.id,
                        title = product.title,
                        priceMicros = product.priceMicros,
                        currencyCode = product.currencyCode,
                        type = ProductTypeData.fromFlatBuffer(product.type),
                    )
                }
            }

            ProductsSectionData(
                products = products,
                fetchedAtMs = section.fetchedAtMs,
            )
        } catch (e: Throwable) {
            errorLog(e) { "Error parsing FlatBuffers products section" }
            null
        }
    }
}
