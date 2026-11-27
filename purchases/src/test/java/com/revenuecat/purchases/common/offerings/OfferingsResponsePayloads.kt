package com.revenuecat.purchases.common.offerings

/**
 * Builds offerings responses shaped like the fallback host's: a fixed number of offerings, each carrying a
 * full `paywall_components` tree localized into many locales, which is where nearly all the bytes go.
 */
internal object OfferingsResponsePayloads {

    const val OFFERING_COUNT = 56
    private const val LOCALE_COUNT = 25
    private const val LOCALIZED_TEXT = "Unlock everything with a subscription that renews until you cancel it"

    /** At least [targetBytes] long (ASCII, so chars equal bytes), and never fewer than [OFFERING_COUNT] offerings. */
    fun withHeavyPaywallComponents(targetBytes: Int): String {
        val bytesPerOffering = targetBytes / OFFERING_COUNT
        val builder = StringBuilder(targetBytes + targetBytes / 8 + 64 * 1024)
        builder.append("{\"current_offering_id\":\"offering_0\",\"offerings\":[")
        repeat(OFFERING_COUNT) { index ->
            if (index > 0) builder.append(',')
            builder.appendOffering(index, bytesPerOffering)
        }
        builder.append("],\"placements\":{\"fallback_offering_id\":\"offering_0\"}}")
        return builder.toString()
    }

    private fun StringBuilder.appendOffering(index: Int, targetBytes: Int) {
        val start = length
        append("{\"identifier\":\"offering_").append(index).append("\",")
        append("\"description\":\"Offering ").append(index).append("\",")
        append("\"metadata\":{\"tier\":\"premium\"},")
        append("\"packages\":[{\"identifier\":\"\$rc_monthly\",\"platform_product_identifier\":\"prod_")
        append(index).append("\"}],")
        append("\"paywall_components\":{")
        append("\"template_name\":\"components\",")
        append("\"asset_base_url\":\"https://assets.pawwalls.com\",")
        append("\"default_locale\":\"locale_0\",")
        append("\"revision\":3,")
        append("\"components_config\":{\"base\":{\"background\":{\"type\":\"color\",\"value\":")
        append("{\"light\":{\"type\":\"hex\",\"value\":\"#ffffff\"}}},")
        append("\"stack\":{\"type\":\"stack\",\"components\":[")
        repeat(LOCALE_COUNT) { component ->
            if (component > 0) append(',')
            append("{\"type\":\"text\",\"text_lid\":\"text_").append(component)
            append("\",\"color\":{\"light\":{\"type\":\"hex\",\"value\":\"#000000\"}},\"size\":{\"width\":")
            append("{\"type\":\"fill\"},\"height\":{\"type\":\"fit\"}}}")
        }
        append("]}}},")
        append("\"components_localizations\":{")
        var textIndex = 0
        do {
            repeat(LOCALE_COUNT) { locale ->
                if (locale > 0 || textIndex > 0) append(',')
                append("\"locale_").append(locale).append('_').append(textIndex).append("\":{")
                append("\"text_").append(textIndex).append("\":\"").append(LOCALIZED_TEXT).append("\",")
                append("\"cta_").append(textIndex).append("\":\"").append(LOCALIZED_TEXT).append("\"}")
            }
            textIndex++
        } while (length - start < targetBytes)
        append("}}}")
    }
}
