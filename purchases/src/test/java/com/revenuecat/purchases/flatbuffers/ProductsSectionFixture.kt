package com.revenuecat.purchases.flatbuffers

import android.util.Base64
import com.google.flatbuffers.FlatBufferBuilder
import com.revenuecat.purchases.flatbuffers.generated.Product
import com.revenuecat.purchases.flatbuffers.generated.ProductsSection

/**
 * Demonstrates the symmetric "backend side": how a producer would build the FlatBuffer with
 * [FlatBufferBuilder] and base64-encode it for embedding in a JSON field. Used by the parser
 * roundtrip test in place of a real backend.
 */
internal data class FixtureProduct(
    val id: String,
    val title: String?,
    val priceMicros: Long,
    val currencyCode: String?,
    val type: Byte,
)

internal fun encodeProductsSectionBase64(
    products: List<FixtureProduct>,
    fetchedAtMs: Long,
): String {
    val builder = FlatBufferBuilder(INITIAL_BUFFER_BYTES)

    // Strings and child tables must be created before the table/vector that references them.
    val productOffsets = products.map { product ->
        val idOffset = builder.createString(product.id)
        val titleOffset = product.title?.let { builder.createString(it) } ?: 0
        val currencyOffset = product.currencyCode?.let { builder.createString(it) } ?: 0
        Product.createProduct(
            builder,
            idOffset,
            titleOffset,
            product.priceMicros,
            currencyOffset,
            product.type,
        )
    }.toIntArray()

    val productsVector = ProductsSection.createProductsVector(builder, productOffsets)
    val root = ProductsSection.createProductsSection(builder, productsVector, fetchedAtMs)
    builder.finish(root)

    return Base64.encodeToString(builder.sizedByteArray(), Base64.NO_WRAP)
}

private const val INITIAL_BUFFER_BYTES = 256
