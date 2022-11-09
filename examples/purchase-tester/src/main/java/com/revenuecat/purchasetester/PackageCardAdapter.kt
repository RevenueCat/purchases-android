package com.revenuecat.purchasetester

import android.annotation.SuppressLint
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
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.googleProduct
import com.revenuecat.purchases_sample.R
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
            val product = currentPackage.product
            binding.currentPackage = currentPackage
            binding.isSubscription = product.type == ProductType.SUBS
            binding.isActive = activeSubscriptions.contains(product.productId)

            binding.packageBuyButton.setOnClickListener {
                val errorStartingPurchase = validateStartPurchase(product)
                if (errorStartingPurchase == null) {
                    listener.onPurchasePackageClicked(
                        binding.root,
                        currentPackage,
                        getSelectedPurchaseOption()
                    )
                } else {
                    showErrorMessage(errorStartingPurchase)
                }
            }

            binding.productBuyButton.setOnClickListener {
                val errorStartingPurchase = validateStartPurchase(product)
                if (errorStartingPurchase == null) {
                    listener.onPurchaseProductClicked(
                        binding.root,
                        product,
                        getSelectedPurchaseOption()
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

            bindPurchaseOptions(product)

            binding.root.setOnClickListener {
                with(binding.packageDetailsContainer) {
                    visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
                }
            }
        }

        @SuppressLint("SetTextI18n")
        private fun bindPurchaseOptions(product: StoreProduct) {
            binding.packagePurchaseOptionGroup.removeAllViews()
            val numberOfPurchaseOptions = product.purchaseOptions.size
            product.purchaseOptions.forEach { purchaseOption ->
                val radioButton = RadioButton(binding.root.context).apply {
                    text = purchaseOption.toButtonString()
                    tag = purchaseOption
                    if (numberOfPurchaseOptions == 1) isChecked = true
                }
                binding.packagePurchaseOptionGroup.addView(radioButton)
            }
        }

        private fun getSelectedPurchaseOption(): PurchaseOption? {
            val selectedButtonId = binding.packagePurchaseOptionGroup.checkedRadioButtonId
            return binding.packagePurchaseOptionGroup.children
                .filter { it.id == selectedButtonId }
                .firstOrNull()
                ?.tag as? PurchaseOption
        }

        private fun validateStartPurchase(product: StoreProduct): String? {
            if (product.type == ProductType.SUBS && binding.packagePurchaseOptionGroup.checkedRadioButtonId != -1) {
                return "Please choose purchase option first"
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
        fun onPurchasePackageClicked(cardView: View, currentPackage: Package, purchaseOption: PurchaseOption?)
        fun onPurchaseProductClicked(cardView: View, currentProduct: StoreProduct, purchaseOption: PurchaseOption?)
    }
}
