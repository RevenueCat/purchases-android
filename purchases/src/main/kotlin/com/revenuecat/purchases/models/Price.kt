package com.revenuecat.purchases.models

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

@Parcelize
@Poko
public class Price(

    /**
     * Formatted price of the item, including its currency sign. For example $3.00.
     */
    public val formatted: String,

    /**
     * Price in micro-units, where 1,000,000 micro-units equal one unit of the currency.
     *
     * For example, if price is "€7.99", price_amount_micros is 7,990,000. This value represents
     * the localized, rounded price for a particular currency.
     */
    public val amountMicros: Long,

    /**
     * Returns ISO 4217 currency code for price and original price.
     *
     * For example, if price is specified in British pounds sterling, price_currency_code is "GBP".
     *
     * If currency code cannot be determined, currency symbol is returned.
     */
    public val currencyCode: String,
) : Parcelable

internal object PriceSerializer : KSerializer<Price> {
    private const val FORMATTED_INDEX = 0
    private const val AMOUNT_MICROS_INDEX = 1
    private const val CURRENCY_CODE_INDEX = 2

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Price") {
        element("formatted", String.serializer().descriptor)
        element("amount_micros", Long.serializer().descriptor)
        element("currency_code", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: Price) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, FORMATTED_INDEX, value.formatted)
            encodeLongElement(descriptor, AMOUNT_MICROS_INDEX, value.amountMicros)
            encodeStringElement(descriptor, CURRENCY_CODE_INDEX, value.currencyCode)
        }
    }

    override fun deserialize(decoder: Decoder): Price {
        return decoder.decodeStructure(descriptor) {
            var formatted = ""
            var amountMicros = 0L
            var currencyCode = ""

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    FORMATTED_INDEX -> formatted = decodeStringElement(descriptor, FORMATTED_INDEX)
                    AMOUNT_MICROS_INDEX -> amountMicros = decodeLongElement(descriptor, AMOUNT_MICROS_INDEX)
                    CURRENCY_CODE_INDEX -> currencyCode = decodeStringElement(descriptor, CURRENCY_CODE_INDEX)
                    -1 -> break
                    else -> error("Unexpected index: $index")
                }
            }

            Price(formatted, amountMicros, currencyCode)
        }
    }
}
