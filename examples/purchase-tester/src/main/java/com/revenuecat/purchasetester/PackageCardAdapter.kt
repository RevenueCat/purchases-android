package com.revenuecat.purchasetester

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases_sample.databinding.PackageCardBinding

class PackageCardAdapter(
    private val packages: List<Package>,
    private val activeSubscriptions: Set<String>,
    private val listener: PackageCardAdapterListener
) :
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
            binding.isActive = activeSubscriptions.contains(currentPackage.product.productId)

            binding.packageBuyButton.setOnClickListener {
                listener.onPurchasePackageClicked(binding.root, currentPackage)
            }

            binding.productBuyButton.setOnClickListener {
                listener.onPurchaseProductClicked(binding.root, currentPackage.product)
            }

            binding.packageType.detail = if (currentPackage.packageType == PackageType.CUSTOM) {
                "custom -> ${currentPackage.packageType.identifier}"
            } else {
                currentPackage.packageType.toString()
            }

            binding.packageDetailsJsonObject.detail = ""
                // TODOBC5 currentPackage.product.originalJson.toString(JSON_FORMATTER_INDENT_SPACES)

            binding.root.setOnClickListener {
                with(binding.packageDetailsContainer) {
                    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
                }
            }
        }
    }

    interface PackageCardAdapterListener {
        fun onPurchasePackageClicked(cardView: View, currentPackage: Package)
        fun onPurchaseProductClicked(cardView: View, currentProduct: StoreProduct)
    }
}
