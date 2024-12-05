package com.revenuecat.purchases.ui.revenuecatui.components.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.revenuecat.purchases.Package
import dev.drewhamilton.poko.Poko

@Stable
internal class PackageContext(
    initialSelectedPackage: Package?,
    initialVariableContext: VariableContext,
) {

    @Poko
    @Immutable
    class VariableContext private constructor(
        @get:JvmSynthetic
        val mostExpensivePricePerMonthMicros: Long? = null,
        @get:JvmSynthetic
        val showZeroDecimalPlacePrices: Boolean = true,
    ) {
        private companion object {
            // FIXME TEST CANDIDATE!
            fun List<Package>.mostExpensivePricePerMonthMicros(): Long? =
                asSequence()
                    .map { pkg -> pkg.product }
                    .mapNotNull { product -> product.pricePerMonth() }
                    .maxByOrNull { price -> price.amountMicros }
                    ?.amountMicros
        }

        constructor(packages: List<Package>, showZeroDecimalPlacePrices: Boolean = true) : this(
            mostExpensivePricePerMonthMicros = packages.mostExpensivePricePerMonthMicros(),
            showZeroDecimalPlacePrices = showZeroDecimalPlacePrices,
        )
    }

    @get:JvmSynthetic
    var selectedPackage by mutableStateOf<Package?>(initialSelectedPackage)
        private set

    @get:JvmSynthetic
    var variableContext by mutableStateOf<VariableContext>(initialVariableContext)
        private set

    @JvmSynthetic
    fun update(selectedPackage: Package?, variableContext: VariableContext) {
        this.selectedPackage = selectedPackage
        this.variableContext = variableContext
    }
}
