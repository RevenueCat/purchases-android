package com.revenuecat.purchasetester

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.net.toUri
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
import com.revenuecat.purchases_sample.databinding.RowViewBinding

class PackageCardAdapter(
    private val packages: List<Package>,
    private val activeSubscriptions: Set<String>,
    private val listener: PackageCardAdapterListener,
    private val isPlayStore: Boolean,
) :
    RecyclerView.Adapter<PackageCardAdapter.PackageViewHolder>() {

    private var isAddOnMode = false
    private val selectedPackages = mutableSetOf<Package>()
    private val selectedSubscriptionOptionsForPackageID = mutableMapOf<String, SubscriptionOption>()
    private var baseProduct: Package? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = PackageCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageViewHolder(binding)
    }

    override fun getItemCount(): Int = packages.size

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(packages[position], isPlayStore, isAddOnMode, selectedPackages.contains(packages[position]))
    }

    fun setAddOnMode(enabled: Boolean) {
        isAddOnMode = enabled
        if (!enabled) {
            selectedPackages.clear()
            selectedSubscriptionOptionsForPackageID.clear()
            baseProduct = null
        }
        notifyDataSetChanged()
        notifySelectionChanged()
    }

    fun getSelectedPackages(): List<Package> = selectedPackages.toList()

    fun getSelectedSubscriptionOptionsForPackageID(): Map<String, SubscriptionOption> =
        selectedPackages
            .mapNotNull { pkg ->
                selectedSubscriptionOptionsForPackageID[pkg.identifier]?.let { pkg.identifier to it }
            }
            .toMap()

    fun getBaseProduct(): Package? = baseProduct

    private fun setBaseProduct(pkg: Package?) {
        baseProduct = pkg
    }

    private fun notifySelectionChanged() {
        val hasSelectedPackages = selectedPackages.isNotEmpty()
        val hasBaseProduct = baseProduct != null
        val baseProductIsSelected = baseProduct != null && selectedPackages.contains(baseProduct)
        listener.onSelectionChanged(hasSelectedPackages, hasBaseProduct && baseProductIsSelected)
    }

    inner class PackageViewHolder(private val binding: PackageCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val nothingCheckedIndex = -1

        @Suppress("CyclomaticComplexMethod", "LongMethod")
        fun bind(currentPackage: Package, isPlayStore: Boolean, isAddOnMode: Boolean, isSelected: Boolean) {
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

            binding.packageSubscriptionOptionTitle.visibility =
                if (isSubscription && isPlayStore) View.VISIBLE else View.GONE
            binding.packageSubscriptionOptionGroup.visibility =
                if (isSubscription && isPlayStore) View.VISIBLE else View.GONE

            binding.buyOptionCheckbox.visibility = if (isAddOnMode) View.VISIBLE else View.GONE
            binding.baseProductCheckbox.visibility = if (isAddOnMode) View.VISIBLE else View.GONE
            binding.isUpgradeCheckbox.visibility = if (isAddOnMode) View.GONE else View.VISIBLE
            binding.isPersonalizedCheckbox.visibility = if (isAddOnMode) View.GONE else View.VISIBLE

            binding.isUpgradeCheckbox.isEnabled = isPlayStore
            binding.isPersonalizedCheckbox.isEnabled = isPlayStore

            binding.packageBuyButton.visibility = if (isAddOnMode) View.GONE else View.VISIBLE
            binding.productBuyButton.visibility = if (isAddOnMode) View.GONE else View.VISIBLE
            binding.optionBuyButton.visibility = if (isPlayStore && !isAddOnMode) View.VISIBLE else View.INVISIBLE
            binding.wplBuyButton.visibility = if (currentPackage.webCheckoutURL == null) View.GONE else View.VISIBLE

            binding.buyOptionCheckbox.setOnCheckedChangeListener(null)
            binding.buyOptionCheckbox.isChecked = isSelected
            binding.buyOptionCheckbox.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedPackages.add(currentPackage)
                } else {
                    selectedPackages.remove(currentPackage)
                    selectedSubscriptionOptionsForPackageID.remove(currentPackage.identifier)
                }
                notifySelectionChanged()
            }

            val isBaseProduct = baseProduct == currentPackage
            binding.baseProductCheckbox.setOnCheckedChangeListener(null)
            binding.baseProductCheckbox.isChecked = isBaseProduct
            binding.baseProductCheckbox.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    setBaseProduct(currentPackage)
                    // Post a refresh to avoid recursion during binding
                    binding.root.post { notifyDataSetChanged() }
                } else if (isBaseProduct) {
                    setBaseProduct(null)
                }
                notifySelectionChanged()
            }

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

            binding.wplBuyButton.setOnClickListener {
                val webCheckoutUrl = currentPackage.webCheckoutURL ?: return@setOnClickListener
                val intent = Intent(Intent.ACTION_VIEW, webCheckoutUrl.toString().toUri())
                binding.root.context.startActivity(intent)
            }

            binding.packageDetailsJsonObject.updateRowView(
                "Product JSON",
                product.googleProduct?.productDetails?.toString()
                    ?: product.amazonProduct?.originalProductJSON.toString(),
            )

            bindSubscriptionOptions(currentPackage)

            binding.root.setOnClickListener {
                with(binding.packageDetailsContainer) {
                    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
                }
            }
        }

        private fun bindSubscriptionOptions(currentPackage: Package) {
            val product = currentPackage.product
            binding.packageSubscriptionOptionGroup.setOnCheckedChangeListener(null)
            binding.packageSubscriptionOptionGroup.removeAllViews()
            val numberOfSubscriptionOptions = product.subscriptionOptions?.size ?: 0
            val defaultOption = product.defaultOption
            product.subscriptionOptions?.forEach { subscriptionOption ->
                val radioButton = RadioButton(binding.root.context).apply {
                    text = subscriptionOption.toButtonString(subscriptionOption == defaultOption)
                    tag = subscriptionOption
                }
                binding.packageSubscriptionOptionGroup.addView(radioButton)
            }

            val existingSelection = selectedSubscriptionOptionsForPackageID[currentPackage.identifier]
            val optionToSelect = existingSelection ?: if (numberOfSubscriptionOptions == 1) {
                product.subscriptionOptions?.firstOrNull()
            } else {
                null
            }

            optionToSelect?.let { subscriptionOption ->
                val radioButton = binding.packageSubscriptionOptionGroup.children
                    .mapNotNull { it as? RadioButton }
                    .firstOrNull { (it.tag as? SubscriptionOption) == subscriptionOption }
                radioButton?.let {
                    it.isChecked = true
                    selectedSubscriptionOptionsForPackageID[currentPackage.identifier] = subscriptionOption
                }
            } ?: run {
                if (numberOfSubscriptionOptions == 0) {
                    selectedSubscriptionOptionsForPackageID.remove(currentPackage.identifier)
                }
            }

            binding.packageSubscriptionOptionGroup.setOnCheckedChangeListener { _, checkedId ->
                val selectedButton = binding.packageSubscriptionOptionGroup.children
                    .firstOrNull { it.id == checkedId } as? RadioButton
                val subscriptionOption = selectedButton?.tag as? SubscriptionOption
                if (subscriptionOption != null) {
                    selectedSubscriptionOptionsForPackageID[currentPackage.identifier] = subscriptionOption
                } else {
                    selectedSubscriptionOptionsForPackageID.remove(currentPackage.identifier)
                }
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

        private fun RowViewBinding.updateRowView(header: String, detail: String?) {
            headerView.text = header
            value.text = detail ?: "None"
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
        fun onAddOnPurchaseClicked(
            selectedPackages: List<Package>,
            selectedSubscriptionOptionsForPackageID: Map<String, SubscriptionOption>,
        )
        fun onSelectionChanged(hasSelectedPackages: Boolean, hasValidBaseProduct: Boolean)
    }
}
