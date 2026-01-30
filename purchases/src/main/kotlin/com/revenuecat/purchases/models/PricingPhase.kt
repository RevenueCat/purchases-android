package com.revenuecat.purchases.models

import android.os.Parcelable
import com.revenuecat.purchases.utils.pricePerDay
import com.revenuecat.purchases.utils.pricePerMonth
import com.revenuecat.purchases.utils.pricePerWeek
import com.revenuecat.purchases.utils.pricePerYear
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.util.Locale

/**
 * Encapsulates how a user pays for a subscription at a given point in time.
 */
@Parcelize
@Poko
class PricingPhase(
    /**
     * Billing period for which the [PricingPhase] applies.
     */
    val billingPeriod: Period,

    /**
     * [RecurrenceMode] of the [PricingPhase]
     */
    val recurrenceMode: RecurrenceMode,

    /**
     * Number of cycles for which the pricing phase applies.
     * Null for INFINITE_RECURRING or NON_RECURRING recurrence modes.
     */
    val billingCycleCount: Int?,

    /**
     * [Price] of the [PricingPhase]
     */
    val price: Price,
) : Parcelable {

    /**
     * Indicates how the pricing phase is charged for FINITE_RECURRING pricing phases
     */
    val offerPaymentMode: OfferPaymentMode?
        get() {
            // billingCycleCount is null for INFINITE_RECURRING or NON_RECURRING recurrence modes
            // but validating for FINITE_RECURRING anyway
            if (recurrenceMode != RecurrenceMode.FINITE_RECURRING) {
                return null
            }

            return if (price.amountMicros == 0L) {
                OfferPaymentMode.FREE_TRIAL
            } else if (billingCycleCount == 1) {
                OfferPaymentMode.SINGLE_PAYMENT
            } else if (billingCycleCount != null && billingCycleCount > 1) {
                OfferPaymentMode.DISCOUNTED_RECURRING_PAYMENT
            } else {
                null
            }
        }

    /**
     * Gives the price of the [PricingPhase] in the given locale in a daily recurrence. This means that for example,
     * if the period is weekly, the price will be divided by 7. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @JvmOverloads
    fun pricePerDay(locale: Locale = Locale.getDefault()): Price =
        price.pricePerDay(billingPeriod, locale)

    /**
     * Gives the price of the [PricingPhase] in the given locale in a weekly recurrence. This means that for example,
     * if the period is monthly, the price will be divided by 4. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @JvmOverloads
    fun pricePerWeek(locale: Locale = Locale.getDefault()): Price =
        price.pricePerWeek(billingPeriod, locale)

    /**
     * Gives the price of the [PricingPhase] in the given locale in a monthly recurrence. This means that for example,
     * if the period is annual, the price will be divided by 12. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @JvmOverloads
    fun pricePerMonth(locale: Locale = Locale.getDefault()): Price =
        price.pricePerMonth(billingPeriod, locale)

    /**
     * Gives the price of the [PricingPhase] in the given locale in a yearly recurrence. This means that for example,
     * if the period is monthly, the price will be multiplied by 12. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @JvmOverloads
    fun pricePerYear(locale: Locale = Locale.getDefault()): Price =
        price.pricePerYear(billingPeriod, locale)

    /**
     * Gives the price of the [PricingPhase] in the given locale in a monthly recurrence. This means that for example,
     * if the period is annual, the price will be divided by 12. It uses a currency formatter to format the price in
     * the given locale. Note that this value may be an approximation.
     *
     * This is equivalent to:
     * ```kotlin
     * pricePerMonth(locale).formatted
     * ```
     *
     * @param locale Locale to use for formatting the price. Default is the system default locale.
     */
    @Deprecated(
        message = "pricePerMonth() provides more price info",
        replaceWith = ReplaceWith("pricePerMonth(locale).formatted"),
    )
    @JvmOverloads
    fun formattedPriceInMonths(locale: Locale = Locale.getDefault()): String {
        return pricePerMonth(locale).formatted
    }
}

internal object PricingPhaseSerializer : KSerializer<PricingPhase> {
    private val nullableIntSerializer = Int.serializer().nullable
    private const val BILLING_PERIOD_INDEX = 0
    private const val RECURRENCE_MODE_INDEX = 1
    private const val BILLING_CYCLE_COUNT_INDEX = 2
    private const val PRICE_INDEX = 3

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PricingPhase") {
        element("billing_period", PeriodSerializer.descriptor)
        element("recurrence_mode", RecurrenceModeSerializer.descriptor)
        element("billing_cycle_count", nullableIntSerializer.descriptor)
        element("price", PriceSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: PricingPhase) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, BILLING_PERIOD_INDEX, PeriodSerializer, value.billingPeriod)
            encodeSerializableElement(descriptor, RECURRENCE_MODE_INDEX, RecurrenceModeSerializer, value.recurrenceMode)
            encodeSerializableElement(
                descriptor,
                BILLING_CYCLE_COUNT_INDEX,
                nullableIntSerializer,
                value.billingCycleCount,
            )
            encodeSerializableElement(descriptor, PRICE_INDEX, PriceSerializer, value.price)
        }
    }

    override fun deserialize(decoder: Decoder): PricingPhase {
        return decoder.decodeStructure(descriptor) {
            var billingPeriod: Period? = null
            var recurrenceMode: RecurrenceMode? = null
            var billingCycleCount: Int? = null
            var price: Price? = null

            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    BILLING_PERIOD_INDEX -> billingPeriod = decodeSerializableElement(
                        descriptor,
                        BILLING_PERIOD_INDEX,
                        PeriodSerializer,
                    )
                    RECURRENCE_MODE_INDEX -> recurrenceMode = decodeSerializableElement(
                        descriptor,
                        RECURRENCE_MODE_INDEX,
                        RecurrenceModeSerializer,
                    )
                    BILLING_CYCLE_COUNT_INDEX -> billingCycleCount = decodeSerializableElement(
                        descriptor,
                        BILLING_CYCLE_COUNT_INDEX,
                        nullableIntSerializer,
                    )
                    PRICE_INDEX -> price = decodeSerializableElement(descriptor, PRICE_INDEX, PriceSerializer)
                    -1 -> break
                    else -> error("Unexpected index: $index")
                }
            }

            PricingPhase(
                billingPeriod = billingPeriod!!,
                recurrenceMode = recurrenceMode!!,
                billingCycleCount = billingCycleCount,
                price = price!!,
            )
        }
    }
}
