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
import com.revenuecat.purchases.amazon.amazonProduct
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases_sample.databinding.PackageCardBinding

class PackageCardAdapter(
    private val packages: List<Package>,
    private val activeSubscriptions: Set<String>,
    private val listener: PackageCardAdapterListener,
    private val isPlayStore: Boolean,
) :
    RecyclerView.Adapter<PackageCardAdapter.PackageViewHolder>() {

    private var isBundleMode = false
    private val selectedPackages = mutableSetOf<Package>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = PackageCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageViewHolder(binding)
    }

    override fun getItemCount(): Int = packages.size

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(packages[position], isPlayStore, isBundleMode, selectedPackages.contains(packages[position]))
    }

    fun setBundleMode(enabled: Boolean) {
        isBundleMode = enabled
        if (!enabled) {
            selectedPackages.clear()
        }
        notifyDataSetChanged()
    }

    fun getSelectedPackages(): List<Package> = selectedPackages.toList()

    inner class PackageViewHolder(private val binding: PackageCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val nothingCheckedIndex = -1

        fun bind(currentPackage: Package, isPlayStore: Boolean, isBundleMode: Boolean, isSelected: Boolean) {
            val product = currentPackage.product
            setupBindingData(currentPackage, product, isPlayStore, isBundleMode, isSelected)
            setupCheckbox(currentPackage, isSelected)
            setupPurchaseButtons(currentPackage, product)
            setupPackageDetails(currentPackage, product)
            bindSubscriptionOptions(product)
            setupRootClickListener()
        }

        private fun setupBindingData(
            currentPackage: Package,
            product: StoreProduct,
            isPlayStore: Boolean,
            isBundleMode: Boolean,
            isSelected: Boolean,
        ) {
            binding.currentPackage = currentPackage
            binding.isSubscription = product.type == ProductType.SUBS
            binding.isActive = activeSubscriptions.contains(product.id)
            binding.isPlayStore = isPlayStore
            binding.isBundleMode = isBundleMode
            binding.isSelected = isSelected
        }

        private fun setupCheckbox(currentPackage: Package, isSelected: Boolean) {
            binding.buyOptionCheckbox.isChecked = isSelected
            binding.buyOptionCheckbox.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedPackages.add(currentPackage)
                } else {
                    selectedPackages.remove(currentPackage)
                }
            }
        }

        private fun setupPurchaseButtons(currentPackage: Package, product: StoreProduct) {
            binding.packageBuyButton.setOnClickListener {
                listener.onPurchasePackageClicked(
                    binding.root,
                    currentPackage,
                    binding.isUpgradeCheckbox.isChecked,
                    binding.isPersonalizedCheckbox.isChecked,
                )
            }

            binding.productBuyButton.setOnClickListener {
                listener.onPurchaseProductClicked(
                    binding.root,
                    product,
                    binding.isUpgradeCheckbox.isChecked,
                    binding.isPersonalizedCheckbox.isChecked,
                )
            }

            binding.optionBuyButton.setOnClickListener {
                val errorStartingPurchase = validateStartPurchase(product)
                val subscriptionOption = getSelectedSubscriptionOption()
                if (subscriptionOption == null) {
                    showErrorMessage("Select a subscription option")
                } else if (errorStartingPurchase == null) {
                    listener.onPurchaseSubscriptionOptionClicked(
                        binding.root,
                        subscriptionOption,
                        binding.isUpgradeCheckbox.isChecked,
                        binding.isPersonalizedCheckbox.isChecked,
                    )
                } else {
                    showErrorMessage(errorStartingPurchase)
                }
            }
        }

        private fun setupPackageDetails(currentPackage: Package, product: StoreProduct) {
            binding.packageType.detail = if (currentPackage.packageType == PackageType.CUSTOM) {
                "custom -> ${currentPackage.packageType.identifier}"
            } else {
                currentPackage.packageType.toString()
            }

            binding.packageDetailsJsonObject.detail = product.googleProduct?.productDetails?.toString()
                ?: product.amazonProduct?.originalProductJSON.toString()
        }

        private fun bindSubscriptionOptions(product: StoreProduct) {
            binding.packageSubscriptionOptionGroup.removeAllViews()
            val numberOfSubscriptionOptions = product.subscriptionOptions?.size ?: 0
            val defaultOption = product.defaultOption
            product.subscriptionOptions?.forEach { subscriptionOption ->
                val radioButton = RadioButton(binding.root.context).apply {
                    text = subscriptionOption.toButtonString(subscriptionOption == defaultOption)
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
                binding.packageSubscriptionOptionGroup.checkedRadioButtonId == nothingCheckedIndex
            ) {
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

        private fun setupRootClickListener() {
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
            isUpgrade: Boolean,
            isPersonalizedPrice: Boolean,
        )
        fun onPurchaseProductClicked(
            cardView: View,
            currentProduct: StoreProduct,
            isUpgrade: Boolean,
            isPersonalizedPrice: Boolean,
        )
        fun onPurchaseSubscriptionOptionClicked(
            cardView: View,
            subscriptionOption: SubscriptionOption,
            isUpgrade: Boolean,
            isPersonalizedPrice: Boolean,
        )
        fun onBundlePurchaseClicked(selectedPackages: List<Package>)
    }
}
