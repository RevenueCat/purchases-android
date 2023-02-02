package com.revenuecat.sample.ui.paywall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.sample.R

class PaywallAdapter(
    var offering: Offering?,
    var didChoosePaywallItem: (PaywallItem) -> Unit
) : RecyclerView.Adapter<PaywallAdapter.PackageViewHolder>() {

    class PackageViewHolder(
        val view: View,
        val didChoosePaywallItem: (PaywallItem) -> Unit
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
                    val price = product.oneTimeProductPrice?.formattedPrice ?: "???"

                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_price).text = price
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_best_offer).visibility = View.GONE
                }
                is PaywallItem.Option -> {
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_options_title).visibility = View.GONE
                    viewHolder.view.findViewById<ConstraintLayout>(R.id.purchasableLayout).visibility = View.VISIBLE

                    val option = it.subscriptionOption
                    val price = option.pricingPhases.map { phase ->
                        "${phase.formattedPrice} for ${phase.billingPeriod}"
                    }.joinToString(separator = " -> ")

                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_price).text = price
                    viewHolder.view.findViewById<TextView>(R.id.paywall_item_best_offer).visibility =  if (it.defaultOffer) View.VISIBLE else View.GONE
                }
            }
        }
    }

    override fun getItemCount() = theItems.size ?: 0

    private val theItems: List<PaywallItem>
        get() = offering?.availablePackages?.flatMap {

            if (it.product.subscriptionOptions.isEmpty()) {
                listOf(
                    PaywallItem.Title(it.product.title),
                    PaywallItem.Product(it.product)
                )
            } else {
                val product = it.product
                val title = product.defaultOption?.pricingPhases?.lastOrNull()?.billingPeriod?.let { period ->
                    when (period) {
                        "P1W" -> "Weekly"
                        "P4W" -> "Every 4  Weeks"
                        "P1M" -> "Monthly"
                        "P3M" -> "Every 3 Months"
                        "P6M" -> "Every 6 Months"
                        "P1Y" -> "Yearly"
                        else -> {
                            null
                        }
                    }
                } ?: product.title

                mutableListOf(PaywallItem.Title(title)) + it.product.subscriptionOptions.map { option ->
                    PaywallItem.Option(option, option == it.product.defaultOption)
                }.sortedBy { option -> !option.defaultOffer }
            }
        } ?: emptyList()
}

sealed class PaywallItem {
    data class Title(
        val title: String
    ) : PaywallItem()

    data class Product(
        val storeProduct: StoreProduct
    ) : PaywallItem()

    data class Option(
        val subscriptionOption: SubscriptionOption,
        val defaultOffer: Boolean
    ) : PaywallItem()
}