package com.revenuecat.purchasetester

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases.models.SubscriptionOption
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

        private val nothingCheckedIndex = -1

        fun bind(currentPackage: Package) {
            val product = currentPackage.product
            binding.currentPackage = currentPackage
            binding.isSubscription = product.type == ProductType.SUBS
            binding.isActive = activeSubscriptions.contains(product.productId)

            binding.packageBuyButton.setOnClickListener {
                listener.onPurchasePackageClicked(
                    binding.root,
                    currentPackage,
                    binding.isUpgradeCheckbox.isChecked
                )
            }

            binding.productBuyButton.setOnClickListener {
                listener.onPurchaseProductClicked(
                    binding.root,
                    product,
                    binding.isUpgradeCheckbox.isChecked
                )
            }

            // TODO add label to default option, update package/product buttons to indicate it will use default
            binding.optionBuyButton.setOnClickListener {
                val errorStartingPurchase = validateStartPurchase(product)
                val subscriptionOption = getSelectedSubscriptionOption()
                if (subscriptionOption == null) {
                    showErrorMessage("Select a subscription option")
                } else if (errorStartingPurchase == null) {
                    listener.onSubscriptionOptionClicked(
                        binding.root,
                        subscriptionOption,
                        binding.isUpgradeCheckbox.isChecked
                    )
                } else {
                    showErrorMessage(errorStartingPurchase)
                }
            }

            binding.packageType.detail = if (currentPackage.packageType == PackageType.CUSTOM) {
                "custom -> ${currentPackage.packageType.identifier}"
            } else {
                currentPackage.packageType.toString()
            }

            binding.packageDetailsJsonObject.detail = product.googleProduct?.productDetails?.toString() ?: "TODO Amazon"

            bindSubscriptionOptions(product)

            binding.root.setOnClickListener {
                with(binding.packageDetailsContainer) {
                    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
                }
            }
        }

        private fun bindSubscriptionOptions(product: StoreProduct) {
            binding.packageSubscriptionOptionGroup.removeAllViews()
            val numberOfSubscriptionOptions = product.subscriptionOptions.size
            product.subscriptionOptions.forEach { subscriptionOption ->
                val radioButton = RadioButton(binding.root.context).apply {
                    text = subscriptionOption.toButtonString()
                    tag = subscriptionOption
                }
                binding.packageSubscriptionOptionGroup.addView(radioButton)
                if (numberOfSubscriptionOptions == 1) binding.packageSubscriptionOptionGroup.check(radioButton.id)
            }
        }

        private fun getSelectedSubscriptionOption(): SubscriptionOption? {
            val selectedButtonId = binding.packageSubscriptionOptionGroup.checkedRadioButtonId
            return binding.packageSubscriptionOptionGroup.children
                .filter { it.id == selectedButtonId }
                .firstOrNull()
                ?.tag as? SubscriptionOption
        }

        private fun validateStartPurchase(product: StoreProduct): String? {
            if (product.type == ProductType.SUBS &&
                binding.packageSubscriptionOptionGroup.checkedRadioButtonId == nothingCheckedIndex) {
                return "Please choose subscription option first"
            }
            return null
        }

        private fun showErrorMessage(errorMessage: String) {
            MaterialAlertDialogBuilder(binding.root.context)
                .setMessage(errorMessage)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    interface PackageCardAdapterListener {
        fun onPurchasePackageClicked(
            cardView: View,
            currentPackage: Package,
            isUpgrade: Boolean
        )
        fun onPurchaseProductClicked(
            cardView: View,
            currentProduct: StoreProduct,
            isUpgrade: Boolean
        )
        fun onSubscriptionOptionClicked(
            cardView: View,
            subscriptionOption: SubscriptionOption,
            isUpgrade: Boolean
        )
    }
}
