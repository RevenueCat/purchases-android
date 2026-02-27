package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.icons.CircleOutlined

@Composable
internal fun DefaultProductCell(
    pkg: Package,
    accentColor: Color,
    selectedFontColor: Color,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "productCellBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) selectedFontColor else MaterialTheme.colorScheme.onSurface,
        label = "productCellContent",
    )

    Row(
        modifier = modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else CircleOutlined,
            contentDescription = if (isSelected) "Selected" else "Not selected",
            tint = contentColor.copy(alpha = if (isSelected) 1f else 0.5f),
        )

        Text(
            text = pkg.product.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = pkg.product.price.formatted,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
    }
}
