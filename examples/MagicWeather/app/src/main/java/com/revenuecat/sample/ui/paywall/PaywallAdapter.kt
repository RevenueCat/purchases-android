package com.revenuecat.sample.ui.paywall

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.sample.R

class PaywallAdapter(
    var offering: Offering?,
    var didChoosePackage: (Package) -> Unit
) : RecyclerView.Adapter<PaywallAdapter.PackageViewHolder>() {

    class PackageViewHolder(
        val view: View,
        val didChoosePackage: (Package) -> Unit
    ) : RecyclerView.ViewHolder(view), View.OnClickListener {
        var item: Package? = null

        init {
            view.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            item?.let {
                didChoosePackage(it)
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.paywall_item, viewGroup, false)

        return PackageViewHolder(view, didChoosePackage)
    }

    override fun onBindViewHolder(viewHolder: PackageViewHolder, position: Int) {
        offering?.availablePackages?.get(position)?.let {
            val item = it
            viewHolder.item = item

            val price = item.product.defaultOption?.pricingPhases?.lastOrNull()?.formattedPrice ?: item.product.oneTimeProductPrice?.formattedPrice ?: "???"

            viewHolder.view.findViewById<TextView>(R.id.paywall_item_title).text = item.product.title
            viewHolder.view.findViewById<TextView>(R.id.paywall_item_description).text = item.product.description
            viewHolder.view.findViewById<TextView>(R.id.paywall_item_price).text = price
        }
    }

    override fun getItemCount() = offering?.availablePackages?.size ?: 0
}
