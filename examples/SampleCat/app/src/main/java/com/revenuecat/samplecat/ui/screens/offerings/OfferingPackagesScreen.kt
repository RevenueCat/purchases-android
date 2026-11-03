package com.revenuecat.samplecat.ui.screens.offerings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.model.PurchasableState
import com.revenuecat.samplecat.ui.components.ConceptIntroduction
import com.revenuecat.samplecat.ui.components.ContentBackground
import com.revenuecat.samplecat.ui.components.PurchasableCard
import com.revenuecat.samplecat.ui.theme.RCGreen
import com.revenuecat.samplecat.viewmodel.OfferingsUiState
import com.revenuecat.samplecat.viewmodel.UserViewModel

/**
 * Screen displaying the packages within a selected offering.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferingPackagesScreen(
    userViewModel: UserViewModel,
    offeringId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offeringsState by userViewModel.offeringsState.collectAsState()
    val isPurchasing by userViewModel.isPurchasing.collectAsState()
    val purchasingProductId by userViewModel.purchasingProductId.collectAsState()

    val offering by remember {
        derivedStateOf {
            (offeringsState as? OfferingsUiState.Success)?.offerings?.all?.get(offeringId)
        }
    }
    val packages by remember {
        derivedStateOf { offering?.availablePackages ?: emptyList() }
    }

    val activity = LocalActivity.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(offering?.identifier ?: stringResource(R.string.nav_packages)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            ContentBackground(color = RCGreen)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item {
                    ConceptIntroduction(
                        imageRes = R.drawable.visual_offerings,
                        title = stringResource(R.string.packages_title),
                        description = stringResource(R.string.packages_description),
                    )
                }

                items(packages, key = { it.identifier }) { pkg ->
                    val productId = pkg.product.id
                    val isPurchased = userViewModel.isProductPurchased(productId)
                    val isThisProductPurchasing = purchasingProductId == productId

                    val state = when {
                        isPurchased -> PurchasableState.Purchased
                        isThisProductPurchasing -> PurchasableState.Purchasing
                        isPurchasing -> PurchasableState.PurchasingOtherProduct
                        else -> PurchasableState.ReadyToPurchase
                    }

                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        PurchasableCard(
                            title = pkg.product.title,
                            productId = productId,
                            description = pkg.product.description,
                            state = state,
                            onPurchaseClick = {
                                activity?.let { act ->
                                    userViewModel.purchase(act, pkg)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
