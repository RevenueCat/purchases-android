package com.revenuecat.paywallstester.ui.screens.main.offerings

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.revenuecat.paywallstester.MainActivity
import com.revenuecat.paywallstester.ui.screens.main.customvariables.CustomVariablesEditorDialog
import com.revenuecat.paywallstester.ui.screens.main.customvariables.CustomVariablesHolder
import com.revenuecat.paywallstester.ui.screens.main.customvariables.CustomVariablesViewModel
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.common.workflows.PublishedWorkflow
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.revenuecat.purchases.Package as RCPackage

@SuppressWarnings("LongParameterList")
@Composable
fun OfferingsScreen(
    tappedOnOffering: (Offering) -> Unit,
    tappedOnOfferingFooter: (Offering) -> Unit,
    tappedOnOfferingCondensedFooter: (Offering) -> Unit,
    tappedOnOfferingByPlacement: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfferingsViewModel = viewModel<OfferingsViewModelImpl>(),
) {
    when (val state = viewModel.offeringsState.collectAsStateWithLifecycle().value) {
        is OfferingsState.Error -> ErrorOfferingsScreen(errorState = state, modifier)
        is OfferingsState.Loaded -> OfferingsListScreen(
            offeringsState = state,
            tappedOnNavigateToOffering = { offering ->
                viewModel.markOfferingAsRecent(offering.identifier)
                tappedOnOffering(offering)
            },
            tappedOnNavigateToOfferingFooter = { offering ->
                viewModel.markOfferingAsRecent(offering.identifier)
                tappedOnOfferingFooter(offering)
            },
            tappedOnNavigateToOfferingCondensedFooter = { offering ->
                viewModel.markOfferingAsRecent(offering.identifier)
                tappedOnOfferingCondensedFooter(offering)
            },
            tappedOnNavigateToOfferingByPlacement = tappedOnOfferingByPlacement,
            tappedOnReloadOfferings = { viewModel.refreshOfferings() },
            onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
            onOfferingInteract = { viewModel.markOfferingAsRecent(it.identifier) },
            modifier,
        )
        OfferingsState.Loading -> LoadingOfferingsScreen(modifier)
    }
}

@Composable
private fun ErrorOfferingsScreen(
    errorState: OfferingsState.Error,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = errorState.purchasesError.toString())
    }
}

@Composable
private fun LoadingOfferingsScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(InternalRevenueCatAPI::class, ExperimentalFoundationApi::class)
@Suppress("LongMethod", "LongParameterList", "ViewModelInjection", "CyclomaticComplexMethod")
@Composable
private fun OfferingsListScreen(
    offeringsState: OfferingsState.Loaded,
    tappedOnNavigateToOffering: (Offering) -> Unit,
    tappedOnNavigateToOfferingFooter: (Offering) -> Unit,
    tappedOnNavigateToOfferingCondensedFooter: (Offering) -> Unit,
    tappedOnNavigateToOfferingByPlacement: (String) -> Unit,
    tappedOnReloadOfferings: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOfferingInteract: (Offering) -> Unit,
    modifier: Modifier = Modifier,
) {
    val customVariablesViewModel: CustomVariablesViewModel = viewModel()
    var dropdownExpandedKey by remember { mutableStateOf<String?>(null) }
    var displayPaywallDialogOffering by remember { mutableStateOf<Offering?>(null) }

    val showDialog = remember { mutableStateOf(false) }
    var showCustomVariablesEditor by remember { mutableStateOf(false) }
    var presentedFlow by remember { mutableStateOf<Pair<PublishedWorkflow, UiConfig>?>(null) }
    var presentedUiLessOffering by remember { mutableStateOf<Offering?>(null) }
    var flowError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val openFlow: (String) -> Unit = { workflowId ->
        scope.launch {
            runCatching {
                Purchases.sharedInstance.awaitGetWorkflow(workflowId) to Purchases.sharedInstance.awaitGetUiConfig()
            }
                .onSuccess { presentedFlow = it }
                .onFailure { flowError = it.message ?: it.toString() }
        }
    }
    val openUiLessFlow: (String) -> Unit = { offeringId ->
        offeringsState.offerings.all[offeringId]
            ?.let { presentedUiLessOffering = it }
            ?: run { flowError = "Offering '$offeringId' is not in the offerings." }
    }
    val query = offeringsState.searchQuery.lowercase().trim()
    val flows = offeringsState.flows.filter { query.isEmpty() || it.matches(query) }

    // Filter and group offerings by template
    val groupedOfferings = remember(offeringsState.offerings, offeringsState.searchQuery, offeringsState.flows) {
        // A claimed offering is opened through its flow in the Flows section.
        val claimed = offeringsState.flows.mapNotNull { it.claimedOfferingId }.toSet()
        val allOfferings = offeringsState.offerings.all.values.filterNot { it.identifier in claimed }
        val filtered = if (query.isEmpty()) {
            allOfferings.toList()
        } else {
            allOfferings.filter { offering ->
                offering.identifier.lowercase().contains(query) ||
                    offering.paywall?.templateName?.lowercase()?.contains(query) == true ||
                    offering.paywallComponents?.dataOrNull?.templateName?.lowercase()?.contains(query) == true
            }
        }
        filtered.groupBy { offering ->
            offering.paywallComponents?.dataOrNull?.templateName?.let { "V2 — $it" }
                ?: offering.paywall?.templateName?.let { "Template $it" }
                ?: UNCLAIMED_OFFERINGS
        }.toSortedMap(
            compareBy {
                // Sort: templates first, then V2, then no paywall last
                when {
                    it.startsWith("Template") -> "0_$it"
                    it.startsWith("V2") -> "1_$it"
                    else -> "2_$it"
                }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn {
            // Search bar
            item {
                OutlinedTextField(
                    value = offeringsState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    label = { Text("Search offerings...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                        )
                    },
                    trailingIcon = {
                        if (offeringsState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                )
            }

            item {
                ListItem(
                    headlineContent = { Text("Get offering by placement") },
                    modifier = Modifier.clickable { showDialog.value = true },
                )
                HorizontalDivider()
            }

            val screenFlows = flows.filter { it.uiLessOfferingId == null }
            if (screenFlows.isNotEmpty()) {
                item { SectionHeader("Flows", "Opened by id. Shows the offering each flow is attached to.") }
                items(screenFlows, key = { "flow_${it.id}" }) { flow ->
                    FlowListItem(flow.name ?: flow.id, flow.subtitle, isError = flow.error != null) {
                        openFlow(flow.id)
                    }
                }
            }
            val uiLessFlows = flows.filter { it.uiLessOfferingId != null }
            if (uiLessFlows.isNotEmpty()) {
                item { SectionHeader("UI-less flows", "Return only an offering. Opens that offering, like an app.") }
                items(uiLessFlows, key = { "uiless_${it.id}" }) { flow ->
                    val offeringId = flow.uiLessOfferingId.orEmpty()
                    FlowListItem(flow.name ?: flow.id, "Offering: $offeringId", isError = false) {
                        openUiLessFlow(offeringId)
                    }
                }
            }

            // Recents section
            val recentOfferings = offeringsState.recentOfferingIds.mapNotNull { id ->
                offeringsState.offerings.all[id]
            }
            if (recentOfferings.isNotEmpty() && offeringsState.searchQuery.isEmpty()) {
                item {
                    SectionHeader("Recents")
                }
                items(recentOfferings, key = { "recent_${it.identifier}" }) { offering ->
                    val rowKey = "recent_${offering.identifier}"
                    OfferingRow(
                        offering = offering,
                        isMenuExpanded = dropdownExpandedKey == rowKey,
                        showSubtitle = true,
                        onTap = {
                            tappedOnNavigateToOffering(offering)
                        },
                        onLongPress = { dropdownExpandedKey = rowKey },
                        onNavigate = tappedOnNavigateToOffering,
                        onDisplayAsDialog = {
                            onOfferingInteract(it)
                            displayPaywallDialogOffering = it
                        },
                        onDisplayAsFooter = tappedOnNavigateToOfferingFooter,
                        onDisplayAsCondensedFooter = tappedOnNavigateToOfferingCondensedFooter,
                        onDismissMenu = { dropdownExpandedKey = null },
                    )
                }
                item { HorizontalDivider() }
            }

            val firstPaywallSection = groupedOfferings.keys.firstOrNull { it != UNCLAIMED_OFFERINGS }
            groupedOfferings.forEach { (sectionTitle, offerings) ->
                item(key = "header_$sectionTitle") {
                    when (sectionTitle) {
                        UNCLAIMED_OFFERINGS -> SectionHeader(
                            sectionTitle,
                            "No paywall or flow attached. Opens the fallback.",
                        )
                        firstPaywallSection -> SectionHeader(sectionTitle, "Paywalls attached directly to an offering.")
                        else -> SectionHeader(sectionTitle)
                    }
                }
                items(offerings, key = { it.identifier }) { offering ->
                    val rowKey = offering.identifier
                    val usedBy = offeringsState.flows.filter { it.uses(offering.identifier) }.map { it.name ?: it.id }
                    OfferingRow(
                        offering = offering,
                        subtitleOverride = usedBy.takeIf { it.isNotEmpty() }?.joinToString(prefix = "Used by: "),
                        isMenuExpanded = dropdownExpandedKey == rowKey,
                        onTap = {
                            tappedOnNavigateToOffering(offering)
                        },
                        onLongPress = { dropdownExpandedKey = rowKey },
                        onNavigate = tappedOnNavigateToOffering,
                        onDisplayAsDialog = {
                            onOfferingInteract(it)
                            displayPaywallDialogOffering = it
                        },
                        onDisplayAsFooter = tappedOnNavigateToOfferingFooter,
                        onDisplayAsCondensedFooter = tappedOnNavigateToOfferingCondensedFooter,
                        onDismissMenu = { dropdownExpandedKey = null },
                    )
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FloatingActionButton(
                onClick = { showCustomVariablesEditor = true },
            ) {
                Text(
                    text = "{ }",
                    fontWeight = FontWeight.Bold,
                )
            }
            FloatingActionButton(
                onClick = { tappedOnReloadOfferings() },
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh offerings",
                )
            }
        }
    }

    if (showCustomVariablesEditor) {
        CustomVariablesEditorDialog(
            viewModel = customVariablesViewModel,
            onDismiss = { showCustomVariablesEditor = false },
        )
    }

    if (displayPaywallDialogOffering != null) {
        PaywallDialog(
            PaywallDialogOptions.Builder()
                .setDismissRequest { displayPaywallDialogOffering = null }
                .setOffering(displayPaywallDialogOffering)
                .setCustomVariables(CustomVariablesHolder.customVariables)
                .setListener(object : PaywallListener {
                    override fun onPurchaseStarted(rcPackage: RCPackage) {
                        Log.d("PaywallDialog", "onPurchaseStarted: ${rcPackage.identifier}")
                    }

                    override fun onPurchaseCompleted(
                        customerInfo: CustomerInfo,
                        storeTransaction: StoreTransaction,
                    ) {
                        Log.d("PaywallDialog", "onPurchaseCompleted: ${storeTransaction.productIds}")
                    }

                    override fun onPurchaseError(error: PurchasesError) {
                        Log.e("PaywallDialog", "onPurchaseError: ${error.message}")
                    }

                    override fun onRestoreStarted() {
                        Log.d("PaywallDialog", "onRestoreStarted")
                    }

                    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                        Log.d("PaywallDialog", "onRestoreCompleted: ${customerInfo.activeSubscriptions}")
                    }

                    override fun onRestoreError(error: PurchasesError) {
                        Log.e("PaywallDialog", "onRestoreError: ${error.message}")
                    }

                    override fun onUrlOpened(url: String) {
                        Log.d("PaywallDialog", "onUrlOpened: $url")
                    }
                })
                .build(),
        )
    }

    if (showDialog.value) {
        PlacementDialog(tappedOnNavigateToOfferingByPlacement = tappedOnNavigateToOfferingByPlacement)
    }

    presentedFlow?.let { (workflow, uiConfig) ->
        FullScreenPaywall(
            PaywallOptions.Builder(dismissRequest = { presentedFlow = null })
                .injectedWorkflow(workflow, offeringsState.offerings, uiConfig)
                .setCustomVariables(CustomVariablesHolder.customVariables)
                .build(),
            onDismiss = { presentedFlow = null },
        )
    }

    // The options the SDK's default checkpoint presenter uses: the offering's paywall, or the fallback paywall.
    presentedUiLessOffering?.let { offering ->
        FullScreenPaywall(
            PaywallOptions.Builder(dismissRequest = { presentedUiLessOffering = null })
                .setOffering(offering)
                .setShouldDisplayDismissButton(true)
                .setCustomVariables(CustomVariablesHolder.customVariables)
                .build(),
            onDismiss = { presentedUiLessOffering = null },
        )
    }

    flowError?.let { message ->
        AlertDialog(
            onDismissRequest = { flowError = null },
            confirmButton = { Button(onClick = { flowError = null }) { Text("OK") } },
            title = { Text("Couldn't open flow") },
            text = { Text(message) },
        )
    }
}

@Composable
private fun FullScreenPaywall(options: PaywallOptions, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Paywall(options)
    }
}

@Composable
private fun FlowListItem(title: String, subtitle: String, isError: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = {
            Text(text = subtitle, color = if (isError) MaterialTheme.colorScheme.error else Color.Unspecified)
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private const val UNCLAIMED_OFFERINGS = "Unclaimed offerings"

@Composable
private fun PlacementDialog(
    tappedOnNavigateToOfferingByPlacement: (String) -> Unit,
) {
    val showDialog = remember { mutableStateOf(false) }
    val placementIdentifier = remember { mutableStateOf("") }
    val noPlacementFoundMessage = remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = {
            showDialog.value = false
            noPlacementFoundMessage.value = null
            placementIdentifier.value = ""
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = Color.White,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Please enter text and submit")

                OutlinedTextField(
                    value = placementIdentifier.value,
                    onValueChange = { placementIdentifier.value = it },
                    label = { Text("Enter placement identifier") },
                )

                noPlacementFoundMessage.value?.let {
                    Text(it)
                }

                // Submit Button
                Button(
                    onClick = {
                        val placementId = placementIdentifier.value

                        Purchases.sharedInstance.getOfferingsWith {
                            it.getCurrentOfferingForPlacement(placementId)?.let { offering ->
                                showDialog.value = false
                                tappedOnNavigateToOfferingByPlacement(placementId)
                            } ?: run {
                                noPlacementFoundMessage.value = "No offering found for placement '$placementId'"
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Submit")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, InternalRevenueCatAPI::class)
@Suppress("LongParameterList")
@Composable
private fun OfferingRow(
    offering: Offering,
    isMenuExpanded: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onNavigate: (Offering) -> Unit,
    onDisplayAsDialog: (Offering) -> Unit,
    onDisplayAsFooter: (Offering) -> Unit,
    onDisplayAsCondensedFooter: (Offering) -> Unit,
    onDismissMenu: () -> Unit,
    showSubtitle: Boolean = false,
    subtitleOverride: String? = null,
) {
    val subtitle = subtitleOverride ?: if (showSubtitle) {
        offering.paywall?.let { "Template ${it.templateName}" }
            ?: offering.paywallComponents?.dataOrNull?.templateName?.let { "Components $it" }
            ?: "No paywall"
    } else {
        null
    }

    Box {
        if (isMenuExpanded) {
            DisplayOfferingMenu(
                offering = offering,
                tappedOnNavigateToOffering = onNavigate,
                tappedOnDisplayOfferingAsDialog = onDisplayAsDialog,
                tappedOnDisplayOfferingAsFooter = onDisplayAsFooter,
                tappedOnDisplayOfferingAsCondensedFooter = onDisplayAsCondensedFooter,
                dismissed = onDismissMenu,
            )
        }
        ListItem(
            headlineContent = { Text(text = offering.identifier) },
            supportingContent = subtitle?.let { { Text(text = it) } },
            modifier = Modifier.combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        )
    }
}

@Composable
private fun SectionHeader(title: String, caption: String? = null) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        caption?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun DisplayOfferingMenu(
    offering: Offering,
    tappedOnNavigateToOffering: (Offering) -> Unit,
    tappedOnDisplayOfferingAsDialog: (Offering) -> Unit,
    tappedOnDisplayOfferingAsFooter: (Offering) -> Unit,
    tappedOnDisplayOfferingAsCondensedFooter: (Offering) -> Unit,
    dismissed: () -> Unit,
) {
    val activity = LocalContext.current as MainActivity
    DropdownMenu(expanded = true, onDismissRequest = { dismissed() }) {
        DropdownMenuItem(
            text = { Text(text = "Navigate to paywall") },
            onClick = { tappedOnNavigateToOffering(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as dialog") },
            onClick = { tappedOnDisplayOfferingAsDialog(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as footer") },
            onClick = { tappedOnDisplayOfferingAsFooter(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as condensed footer") },
            onClick = { tappedOnDisplayOfferingAsCondensedFooter(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as activity") },
            onClick = { activity.launchPaywall(offering, edgeToEdge = false) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as activity (edgeToEdge enabled)") },
            onClick = { activity.launchPaywall(offering, edgeToEdge = true) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as view in an activity (Purchase button gating example)") },
            onClick = { activity.launchPaywallViewAsActivity(offering) },
        )
        DropdownMenuItem(
            text = { Text(text = "Display paywall as footer view in an activity") },
            onClick = { activity.launchPaywallFooterViewAsActivity(offering) },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OfferingsScreenPreview() {
    OfferingsScreen(
        tappedOnOffering = {},
        tappedOnOfferingFooter = {},
        tappedOnOfferingCondensedFooter = {},
        tappedOnOfferingByPlacement = {},
        viewModel = object : OfferingsViewModel() {
            private val _offeringsState = MutableStateFlow<OfferingsState>(
                OfferingsState.Loaded(
                    Offerings(
                        current = null,
                        all = emptyMap(),
                    ),
                ),
            )

            override val offeringsState: StateFlow<OfferingsState>
                get() = _offeringsState.asStateFlow()

            override fun refreshOfferings() {
                // no-op
            }

            override fun updateSearchQuery(query: String) {
                // no-op
            }

            override fun markOfferingAsRecent(offeringId: String) {
                // no-op
            }
        },
    )
}
