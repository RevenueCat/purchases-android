package com.revenuecat.purchases.ui.revenuecatui.defaultpaywall

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.icons.CheckCircle
import com.revenuecat.purchases.ui.revenuecatui.helpers.TestTag
import com.revenuecat.purchases.ui.revenuecatui.icons.CircleOutlined

@Suppress("LongParameterList")
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
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .testTag(TestTag.selectButtonTestTag(pkg.identifier))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (isSelected) CheckCircle else CircleOutlined,
            contentDescription = null,
            tint = contentColor.copy(alpha = if (isSelected) 1f else 0.5f),
        )

        Text(
            text = pkg.product.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = pkg.product.price.formatted,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
        )
    }
}
