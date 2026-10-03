package com.revenuecat.purchases.models

import dev.drewhamilton.poko.Poko

/**
 * Google-specific discount display information for a one-time purchase offer.
 */
@Poko
public class GoogleDiscountDisplayInfo(
    override val percentageDiscount: Int? = null,
    override val discountAmount: Price? = null,
) : DiscountDisplayInfo