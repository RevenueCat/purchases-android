package com.revenuecat.sample.ui.paywall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.sample.R

class PaywallAdapter(
    var offering: Offering?,
    var didChoosePaywallItem: (PaywallItem) -> Unit,
) : RecyclerView.Adapter<PaywallAdapter.PackageViewHolder>() {

    class PackageViewHolder(
        val view: View,
        val didChoosePaywallItem: (PaywallItem) -> Unit,
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {
        var item: PaywallItem? = null

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            item?.let {
                didChoosePaywallItem(it)
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.paywall_item, viewGroup, false)

        return PackageViewHolder(view, didChoosePaywallItem)
    }

    override fun onBindViewHolder(viewHolder: PackageViewHolder, position: Int) {
        theItems[position].let {
            val item = it
            viewHolder.item = item

            when (it) {
                is PaywallItem.Title -> {
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_options_title).visibility = View.VISIBLE
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_options_title).text = it.title
                    viewHolder.view.findViewById<ConstraintLayout>(R.id.purchasableLayout).visibility = View.GONE
                }
                is PaywallItem.Product -> {
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_options_title).visibility = View.GONE
                    viewHolder.view.findViewById<ConstraintLayout>(R.id.purchasableLayout).visibility = View.VISIBLE

                    val product = it.storeProduct
                    val price = product.price.formatted

                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_price).text = price
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_best_offer).visibility = View.GONE
                }
                is PaywallItem.Option -> {
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_options_title).visibility = View.GONE
                    viewHolder.view.findViewById<ConstraintLayout>(R.id.purchasableLayout).visibility = View.VISIBLE

                    val option = it.subscriptionOption
                    val price = option.pricingPhases.joinToString(separator = " -> ") { phase ->
                        "${phase.price.formatted} for ${phase.billingPeriod.toDescription}"
                    }

                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_price).text = price
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_best_offer).visibility =
                        if (it.defaultOffer) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun getItemCount() = theItems.size ?: 0

    private val theItems: List<PaywallItem>
        get() = offering?.availablePackages?.flatMap {
            val product = it.product

            it.product.subscriptionOptions?.let { options ->
                val title = product.period?.toTitle ?: product.title

                mutableListOf(PaywallItem.Title(title)) + options.map { option ->
                    PaywallItem.Option(option, option == it.product.defaultOption)
                }.sortedBy { option -> !option.defaultOffer }
            } ?: run {
                listOf(
                    PaywallItem.Title(product.title),
                    PaywallItem.Product(product),
                )
            }
        } ?: emptyList()
}

sealed class PaywallItem {
    data class Title(
        val title: String,
    ) : PaywallItem()

    data class Product(
        val storeProduct: StoreProduct,
    ) : PaywallItem()

    data class Option(
        val subscriptionOption: SubscriptionOption,
        val defaultOffer: Boolean,
    ) : PaywallItem()
}

val Period.toTitle: String?
    get() = when (unit) {
        Period.Unit.DAY -> if (value == 1) "Daily" else "Every $value days"
        Period.Unit.WEEK -> if (value == 1) "Weekly" else "Every $value weeks"
        Period.Unit.MONTH -> if (value == 1) "Monthly" else "Every $value months"
        Period.Unit.YEAR -> if (value == 1) "Yearly" else "Every $value years"
        Period.Unit.UNKNOWN -> "Unknown"
    }

val Period.toDescription: String?
    get() = when (unit) {
        Period.Unit.DAY -> if (value == 1) "$value day" else "$value days"
        Period.Unit.WEEK -> if (value == 1) "$value week" else "$value weeks"
        Period.Unit.MONTH -> if (value == 1) "$value month" else "$value months"
        Period.Unit.YEAR -> if (value == 1) "$value year" else "$value years"
        Period.Unit.UNKNOWN -> null
    }
