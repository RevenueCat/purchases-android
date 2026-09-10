package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialButtonComponent
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedButtonPartial(
    @get:JvmSynthetic val partial: PartialButtonComponent,
) : PresentedPartial<PresentedButtonPartial> {

    companion object {
        @JvmSynthetic
        operator fun invoke(
            from: PartialButtonComponent,
        ): Result<PresentedButtonPartial, NonEmptyList<PaywallValidationError>> =
            Result.Success(PresentedButtonPartial(partial = from))
    }

    override fun combine(with: PresentedButtonPartial?): PresentedButtonPartial {
        val otherPartial = with?.partial
        return PresentedButtonPartial(
            partial = PartialButtonComponent(
                visible = otherPartial?.visible ?: partial.visible,
            ),
        )
    }
}
