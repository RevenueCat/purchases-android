@file:OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.customercenter.views

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.PromotionalOfferData

@JvmSynthetic
@Composable
internal fun PromotionalOfferView(
    promotionalOfferData: PromotionalOfferData,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        AppIconView(modifier = Modifier
            .padding(top = 48.dp, bottom = 16.dp)
            .size(100.dp))

        Text(
            text = promotionalOfferData.promotionalOffer.title,
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = promotionalOfferData.promotionalOffer.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))
//
//                PromoOfferButtonView(
//                    isLoading = isLoading,
//                    product = product,
//                    discount = promotionalOffer.discount,
//                    onPurchaseComplete = { action ->
//                        isLoading = false
//                        onDismissPromotionalOfferView(action)
//                    }
//                )

        Button(
            onClick = { promotionalOfferData.onAccepted() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Text("Accept offer")
        }
        OutlinedButton(
            onClick = { promotionalOfferData.onDismiss() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            Text("No Thanks")
        }
    }
}

@SuppressLint("DiscouragedApi")
@Composable
fun AppIconView(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val icon: Drawable? = try {
        context.packageManager.getApplicationIcon(context.packageName)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }

    icon?.let {
        Image(
            modifier = modifier
                .clip(CircleShape),
            painter = rememberAsyncImagePainter(model = it),
            contentDescription = null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PromotionalOfferViewPreview() {
    val promoOffer = CustomerCenterConfigTestData.customerCenterData()
        .getManagementScreen()?.paths?.first {
            it.type == CustomerCenterConfigData.HelpPath.PathType.REFUND_REQUEST
        }!!.promotionalOffer!!
    val data = PromotionalOfferData(
        promoOffer,
        onAccepted = {},
        onDismiss = {},
    )
    PromotionalOfferView(data)
}
