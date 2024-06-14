package com.revenuecat.purchasetester

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.amazon.amazonProduct
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases_sample.databinding.PackageCardBinding

class DeprecatedPackageCardAdapter(
    private val packages: List<Package>,
    private val activeSubscriptions: Set<String>,
    private val listener: PackageCardAdapterListener,
) :
    RecyclerView.Adapter<DeprecatedPackageCardAdapter.PackageViewHolder>() {

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
            val product = currentPackage.product
            binding.currentPackage = currentPackage
            binding.isSubscription = product.type == ProductType.SUBS
            binding.isActive = activeSubscriptions.contains(product.id)
            // Upgrades are no longer possible with deprecated methods.
            binding.isUpgradeCheckbox.isVisible = false

            binding.packageBuyButton.setOnClickListener {
                listener.onPurchasePackageClicked(
                    binding.root,
                    currentPackage,
                )
            }

            binding.packageBuyButton.text = "Buy package (deprecated)"

            binding.productBuyButton.setOnClickListener {
                listener.onPurchaseProductClicked(
                    binding.root,
                    product,
                )
            }
            binding.productBuyButton.text = "Buy product (deprecated)"

            binding.optionBuyButton.visibility = View.INVISIBLE
            binding.packageSubscriptionOptionGroup.visibility = View.INVISIBLE

            binding.packageType.detail = if (currentPackage.packageType == PackageType.CUSTOM) {
                "custom -> ${currentPackage.packageType.identifier}"
            } else {
                currentPackage.packageType.toString()
            }

            binding.packageDetailsJsonObject.detail = product.googleProduct?.productDetails?.toString()
                ?: product.amazonProduct?.originalProductJSON.toString()

            binding.root.setOnClickListener {
                with(binding.packageDetailsContainer) {
                    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
                }
            }
        }
    }

    interface PackageCardAdapterListener {
        fun onPurchasePackageClicked(
            cardView: View,
            currentPackage: Package,
        )
        fun onPurchaseProductClicked(
            cardView: View,
            currentProduct: StoreProduct,
        )
    }
}
