package com.revenuecat.sample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.sample.databinding.PackageCardBinding

class PackageCardAdapter(private val packages: List<Package>, private val listener: PackageCardAdapterListener) :
    RecyclerView.Adapter<PackageCardAdapter.PackageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = PackageCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageViewHolder(binding)
    }

    override fun getItemCount(): Int = packages.size

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(packages[position])
    }

    inner class PackageViewHolder(private val binding: PackageCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(currentPackage: Package) {
            binding.currentPackage = currentPackage
            binding.packageBuyButton.setOnClickListener {
                listener.onBuyPackageClicked(binding.root, currentPackage)
            }

            binding.packageType.detail = if (currentPackage.packageType == PackageType.CUSTOM) {
                "custom -> ${currentPackage.packageType.identifier}"
            } else {
                currentPackage.packageType.toString()
            }
        }
    }

    interface PackageCardAdapterListener {
        fun onBuyPackageClicked(cardView: View, currentPackage: Package)
    }
}
