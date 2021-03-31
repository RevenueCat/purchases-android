package com.revenuecat.sample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Offering
import com.revenuecat.sample.databinding.OfferingCardBinding

class OfferingCardAdapter(private val offerings: List<Offering>, private val listener: OfferingCardAdapterListener) :
    RecyclerView.Adapter<OfferingCardAdapter.OfferingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferingViewHolder {
        val binding = OfferingCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferingViewHolder(binding)
    }

    override fun getItemCount(): Int = offerings.size

    override fun onBindViewHolder(holder: OfferingViewHolder, position: Int) {
        holder.bind(offerings[position])
    }

    inner class OfferingViewHolder(private val binding: OfferingCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(offering: Offering) {
            binding.offering = offering
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
