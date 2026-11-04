package com.revenuecat.samplecat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Offering
import com.revenuecat.samplecat.R
import com.revenuecat.samplecat.ui.theme.SampleCatTheme

/**
 * A card component for displaying an offering in the offerings list.
 *
 * @param offering The RevenueCat offering to display
 * @param onClick Called when the card is clicked
 */
@Composable
fun OfferingCard(
    offering: Offering,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OfferingCardContent(
        identifier = offering.identifier,
        packageCount = offering.availablePackages.size,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun OfferingCardContent(
    identifier: String,
    packageCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val cardBackground = if (isDarkTheme) Color.Black else Color.White

    val packageText = when (packageCount) {
        0 -> stringResource(R.string.package_count_zero)
        1 -> stringResource(R.string.package_count_one)
        else -> stringResource(R.string.package_count_other, packageCount)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = identifier,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = packageText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OfferingCardPreview() {
    SampleCatTheme {
        OfferingCardContent(
            identifier = "default",
            packageCount = 3,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfferingCardSinglePackagePreview() {
    SampleCatTheme {
        OfferingCardContent(
            identifier = "premium",
            packageCount = 1,
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfferingCardNoPackagesPreview() {
    SampleCatTheme {
        OfferingCardContent(
            identifier = "empty_offering",
            packageCount = 0,
            onClick = {},
        )
    }
}
