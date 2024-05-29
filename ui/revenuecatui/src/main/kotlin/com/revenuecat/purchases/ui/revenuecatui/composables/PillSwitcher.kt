package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PillSwitcherViewModel(
    initialOptions: List<PaywallData.Configuration.Tier>,
    defaultSelectedOption: PaywallData.Configuration.Tier
) : ViewModel() {
    private val _options = MutableStateFlow(initialOptions)
    val options: StateFlow<List<PaywallData.Configuration.Tier>> = _options

    private val _selectedOption = MutableStateFlow(
        if (defaultSelectedOption in initialOptions) defaultSelectedOption else initialOptions.firstOrNull() ?: PaywallData.Configuration.Tier("", emptyList(), "")
    )
    val selectedOption: StateFlow<PaywallData.Configuration.Tier> = _selectedOption

    fun onOptionSelected(option: PaywallData.Configuration.Tier) {
        _selectedOption.value = option
    }

    fun setOptions(newOptions: List<PaywallData.Configuration.Tier>) {
        _options.value = newOptions
        // Update the selected option if necessary to ensure it is within the new options
        if (_selectedOption.value !in newOptions) {
            _selectedOption.value = newOptions.firstOrNull() ?: PaywallData.Configuration.Tier("", emptyList(), "")
        }
    }
}

internal class PillSwitcherViewModelFactory(
    private val initialOptions: List<PaywallData.Configuration.Tier>,
    private val defaultSelectedOption: PaywallData.Configuration.Tier
) : ViewModelProvider.NewInstanceFactory() {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PillSwitcherViewModel(
            initialOptions = initialOptions,
            defaultSelectedOption = defaultSelectedOption,
        ) as T
    }
}

@Composable
fun PillSwitcher(
    viewModel: PillSwitcherViewModel = viewModel(),
    backgroundColor: Color,
) {
    val options by viewModel.options.collectAsState()
    val selectedOption by viewModel.selectedOption.collectAsState()
    val selectedIndex = options.indexOf(selectedOption)
    var totalWidthPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current

    Box(
        Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF0F0F0))
            .height(40.dp)
            .fillMaxWidth()
            .onSizeChanged { size ->
                totalWidthPx = size.width
            }
    ) {
        val optionWidth = with(density) { (totalWidthPx / options.size).toDp() }
        val indicatorOffset by animateDpAsState(targetValue = optionWidth * selectedIndex)

        Box(
            Modifier
                .offset(x = indicatorOffset)
                .fillMaxHeight()
                .width(optionWidth)
                .clip(RoundedCornerShape(50))
                .background(backgroundColor)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            options.forEach { option ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            viewModel.onOptionSelected(option)
                        }
                ) {
                    Text(
                        text = option.id, // TODO: Needs to be localized version of tier name
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = if (selectedOption == option) Color.White else Color.Black
                    )
                }
            }
        }
    }
}
