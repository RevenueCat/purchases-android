package com.revenuecat.purchasetester

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases_sample.databinding.OfferingCardBinding

class OfferingCardAdapter(
    private val offerings: List<Offering>,
    private val currentOffering: Offering?,
    private val listener: OfferingCardAdapterListener,
) :
    RecyclerView.Adapter<OfferingCardAdapter.OfferingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferingViewHolder {
        val binding = OfferingCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferingViewHolder(binding)
    }

    override fun getItemCount(): Int = offerings.size

    override fun onBindViewHolder(holder: OfferingViewHolder, position: Int) {
        val offering = offerings[position]
        holder.bind(offering, offering == currentOffering)
    }

    inner class OfferingViewHolder(private val binding: OfferingCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(offering: Offering, isCurrent: Boolean) {
            binding.offering = offering
            binding.isCurrent = isCurrent
            binding.root.setOnClickListener {
                listener.onOfferingClicked(binding.root, offering)
            }
            ViewCompat.setTransitionName(binding.root, offering.identifier)
        }
    }

    interface OfferingCardAdapterListener {
        fun onOfferingClicked(cardView: View, offering: Offering)
    }
}
