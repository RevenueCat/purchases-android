package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.R

@Composable
fun PromotionalOfferView(
    promotionalOffer: PromotionalOffer,
    product: StoreProduct,
    promoOfferDetails: CustomerCenterConfigData.HelpPath.PromotionalOffer,
    onDismissPromotionalOfferView: (PromotionalOfferViewAction) -> Unit,
) {
    var isLoading by remember { mutableStateOf(false) }
    val viewModel = remember { PromotionalOfferViewModel(promotionalOffer, product, promoOfferDetails) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (viewModel.error == null) {
                AppIconView()
                    .padding(top = 100.dp, bottom = 16.dp)

                PromotionalOfferHeaderView(viewModel)

                Spacer(modifier = Modifier.weight(1f))

                PromoOfferButtonView(
                    isLoading = isLoading,
                    viewModel = viewModel,
                    onPurchaseComplete = { action ->
                        isLoading = false
                        onDismissPromotionalOfferView(action)
                    },
                )

                Button(
                    onClick = { onDismissPromotionalOfferView(PromotionalOfferViewAction.DeclinePromotionalOffer) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text("No Thanks")
                }
            }
        }
    }
}

@Composable
fun AppIconView() {
    val appIcon: Painter = painterResource(id = R.drawable.ic_launcher_foreground) // Replace with actual icon resource
    Image(
        painter = appIcon,
        contentDescription = null,
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Gray),
    )
}

@Composable
fun PromotionalOfferHeaderView(viewModel: PromotionalOfferViewModel) {
    val details = viewModel.promotionalOfferData?.promoOfferDetails
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 40.dp),
    ) {
        Text(
            text = details?.title ?: "",
            style = MaterialTheme.typography.h5,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = details?.subtitle ?: "",
            style = MaterialTheme.typography.body1,
        )
    }
}

@Composable
fun PromoOfferButtonView(
    isLoading: Boolean,
    viewModel: PromotionalOfferViewModel,
    onPurchaseComplete: (PromotionalOfferViewAction) -> Unit,
) {
    val product = viewModel.promotionalOfferData?.product
    val discount = viewModel.promotionalOfferData?.promotionalOffer?.discount

    if (product != null && discount != null) {
        Button(
            onClick = {
                isLoading = true
                // Simulate purchase process
                onPurchaseComplete(
                    PromotionalOfferViewAction.SuccessfullyRedeemedPromotionalOffer(PurchaseResultData()),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            enabled = !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Column {
                    Text(text = discount.localizedPricePerPeriodByPaymentMode())
                    Text(text = "then ${product.localizedPricePerPeriod()}")
                }
            }
        }
    }
}
