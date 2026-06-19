package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialPackageComponent
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import dev.drewhamilton.poko.Poko

@Poko
internal class PresentedPackagePartial(
    @get:JvmSynthetic val partial: PartialPackageComponent,
) : PresentedPartial<PresentedPackagePartial> {

    companion object {
        @JvmSynthetic
        operator fun invoke(
            from: PartialPackageComponent,
        ): Result<PresentedPackagePartial, NonEmptyList<PaywallValidationError>> =
            Result.Success(PresentedPackagePartial(partial = from))
    }

    override fun combine(with: PresentedPackagePartial?): PresentedPackagePartial {
        val otherPartial = with?.partial
        return PresentedPackagePartial(
            partial = PartialPackageComponent(
                visible = otherPartial?.visible ?: partial.visible,
            ),
        )
    }
}
