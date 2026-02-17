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
import com.revenuecat.purchases_sample.databinding.RowViewBinding

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
            val isSubscription = product.type == ProductType.SUBS
            val isActive = activeSubscriptions.contains(product.id)

            binding.packageProductTitle.text = "${product.title}${if (isActive) " (active)" else ""}"
            binding.packageProductDescription.text = product.description

            binding.packageProductSku.updateRowView("Sku:", product.id)
            binding.packageType.updateRowView(
                "Package Type:",
                if (currentPackage.packageType == PackageType.CUSTOM) {
                    "custom -> ${currentPackage.packageType.identifier}"
                } else {
                    currentPackage.packageType.toString()
                },
            )

            binding.packageOneTimePrice.root.visibility = if (isSubscription) View.GONE else View.VISIBLE
            if (!isSubscription) {
                binding.packageOneTimePrice.updateRowView("One Time Price:", product.price.formatted)
            }

            // Upgrades are no longer possible with deprecated methods.
            binding.isUpgradeCheckbox.isVisible = false
            binding.isPersonalizedCheckbox.visibility = View.GONE
            binding.optionBuyButton.visibility = View.INVISIBLE
            binding.packageSubscriptionOptionGroup.visibility = View.INVISIBLE
            binding.packageSubscriptionOptionTitle.visibility = View.GONE
            binding.buyOptionCheckbox.visibility = View.GONE
            binding.baseProductCheckbox.visibility = View.GONE

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

            binding.packageDetailsJsonObject.updateRowView(
                "Product JSON",
                product.googleProduct?.productDetails?.toString()
                    ?: product.amazonProduct?.originalProductJSON.toString(),
            )

            binding.root.setOnClickListener {
                with(binding.packageDetailsContainer) {
                    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
                }
            }
        }

        private fun RowViewBinding.updateRowView(header: String, detail: String?) {
            headerView.text = header
            value.text = detail ?: "None"
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
