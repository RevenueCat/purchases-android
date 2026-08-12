# Paywall Accessibility Traversal Order Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Paywalls V2 expose fixed headers, scrollable bodies, and sticky footers to TalkBack in that exact order.

**Architecture:** Keep `OverlayLayout`'s body-first emission and visual placement, but centralize its three regions as named composable slots. Wrap each emitted slot in a peer semantics traversal group with an explicit index, and make the overlay itself the parent traversal group; both standard and workflow paywalls then inherit one ordering contract.

**Tech Stack:** Kotlin, Jetpack Compose semantics and custom layout, JUnit 4, Robolectric Compose UI tests, AssertJ

## Global Constraints

- Accessibility order is header (`-1f`) → body (`0f`) → sticky footer (`1f`).
- Missing optional regions are skipped without changing the relative order of remaining regions.
- Preserve existing visual stacking, sizing, padding, scrolling, measurement order, and placement order.
- Preserve body-first emission so header and footer continue drawing above it.
- Keep all new APIs private or internal; do not change the published RevenueCatUI API.
- Do not introduce `onSizeChanged` or a new `CompositionLocal`.
- Do not add separate workflow-only traversal logic.

---

### Task 1: Define and verify paywall region traversal order

**Files:**
- Create: `ui/revenuecatui/src/test/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/OverlayLayoutAccessibilityTests.kt`
- Modify: `ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/LoadedPaywallComponents.kt:156-260`
- Modify: `ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/LoadedWorkflowPaywall.kt:240-274`

**Interfaces:**
- Consumes: `PaywallComponentsScaffold`, `PaywallState.Loaded.Components`, `FakePaywallState`, and Compose `SemanticsProperties.IsTraversalGroup`/`TraversalIndex`.
- Produces: an internal `OverlayLayout(state, modifier, headerContent, footerContent, mainContent)` contract that always owns the semantics and indexing of its named regions.

- [ ] **Step 1: Write the failing scaffold semantics test**

Create `OverlayLayoutAccessibilityTests.kt` with a real `PaywallComponentsScaffold`. The production change this test catches is removing or misassigning any region traversal index or removing the overlay traversal-group boundary.

```kotlin
package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class OverlayLayoutAccessibilityTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `header body and footer have explicit traversal order`(): Unit = with(composeTestRule) {
        val state = FakePaywallState()
        setContent {
            PaywallComponentsScaffold(
                state = state,
                modifier = Modifier.fillMaxSize(),
                background = null,
                headerContent = {
                    Box(
                        Modifier
                            .testTag("header")
                            .fillMaxWidth()
                            .height(40.dp),
                    )
                },
                footerContent = {
                    Box(
                        Modifier
                            .testTag("footer")
                            .fillMaxWidth()
                            .height(60.dp),
                    )
                },
            ) {
                Box(Modifier.testTag("body").fillMaxSize())
            }
        }

        val headerGroup = onNodeWithTag("header", useUnmergedTree = true)
            .onParent()
            .assertTraversalGroup(index = -1f)
        val bodyGroup = onNodeWithTag("body", useUnmergedTree = true)
            .onParent()
            .assertTraversalGroup(index = 0f)
        val footerGroup = onNodeWithTag("footer", useUnmergedTree = true)
            .onParent()
            .assertTraversalGroup(index = 1f)

        val overlayGroup = bodyGroup.onParent()
        overlayGroup.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true),
        )
        val overlayId = overlayGroup.fetchSemanticsNode().id
        assertThat(headerGroup.onParent().fetchSemanticsNode().id).isEqualTo(overlayId)
        assertThat(footerGroup.onParent().fetchSemanticsNode().id).isEqualTo(overlayId)
    }

    private fun SemanticsNodeInteraction.assertTraversalGroup(index: Float): SemanticsNodeInteraction =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, index))
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
./gradlew :ui:revenuecatui:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.ui.revenuecatui.components.OverlayLayoutAccessibilityTests"
```

Expected: FAIL because each tagged region's direct parent is the existing ungrouped `OverlayLayout`, so `IsTraversalGroup` and the three distinct `TraversalIndex` values are absent.

- [ ] **Step 3: Implement named traversal slots in `OverlayLayout`**

In `LoadedPaywallComponents.kt`, add these imports:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
```

Replace the footer/no-footer branches inside `PaywallComponentsScaffold` with one named-slot call:

```kotlin
OverlayLayout(
    state = state,
    modifier = Modifier.fillMaxSize(),
    headerContent = headerContent,
    footerContent = footerContent,
    mainContent = mainContent,
)
```

Change `OverlayLayout` to own the named slots and their peer semantics groups while retaining body/header/footer emission and placement order:

```kotlin
@Composable
internal fun OverlayLayout(
    state: PaywallState.Loaded.Components,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    mainContent: @Composable () -> Unit,
) {
    Layout(
        content = {
            PaywallTraversalRegion(index = 0f, content = mainContent)
            headerContent?.let { PaywallTraversalRegion(index = -1f, content = it) }
            footerContent?.let { PaywallTraversalRegion(index = 1f, content = it) }
        },
        modifier = modifier.semantics { isTraversalGroup = true },
    ) { measurables, constraints ->
        val headerMeasurable = if (headerContent != null) measurables[1] else null
        val footerMeasurable = if (footerContent != null) {
            measurables[if (headerContent != null) 2 else 1]
        } else {
            null
        }

        val headerPlaceable = headerMeasurable?.measure(constraints.copy(minHeight = 0))
        val footerPlaceable = footerMeasurable?.measure(constraints.copy(minHeight = 0))

        if (headerContent != null) state.headerHeightPx = headerPlaceable?.height ?: 0
        if (footerContent != null) state.footerHeightPx = footerPlaceable?.height ?: 0

        val mainPlaceable = measurables[0].measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            mainPlaceable.placeRelative(0, 0)
            headerPlaceable?.placeRelative(0, 0)
            footerPlaceable?.placeRelative(0, constraints.maxHeight - footerPlaceable.height)
        }
    }
}

@Composable
private fun PaywallTraversalRegion(
    index: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.semantics {
            isTraversalGroup = true
            traversalIndex = index
        },
        propagateMinConstraints = true,
    ) {
        content()
    }
}
```

Update the `OverlayLayout` KDoc to describe named slots and the explicit header/body/footer accessibility order. Retain the comments explaining overlay-first measurement and why nested workflow layouts only publish heights for overlays they own.

In `LoadedWorkflowPaywall.kt`, replace the positional `hasFooter` call with named `footerContent` and keep the body as the trailing `mainContent` lambda:

```kotlin
OverlayLayout(
    state = stepState,
    modifier = Modifier.fillMaxSize(),
    footerContent = stepState.stickyFooter?.let { footerStyle ->
        {
            ComponentView(
                style = footerStyle,
                state = stepState,
                onClick = onClick,
                componentInteractionTracker = tracker,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    },
) {
    ComponentView(
        style = stepState.stack,
        state = stepState,
        onClick = onClick,
        componentInteractionTracker = tracker,
        modifier = Modifier
            .fillMaxSize()
            .conditional(shouldWrapMainContentInVerticalScroll) {
                verticalScroll(mainScrollState)
            }
            .conditional(stepState.header != null && !stepState.mainStackHasHeroImage) {
                headerTopPadding(stepState)
            }
            .conditional(stepState.stickyFooter != null) {
                footerBottomPadding(stepState)
            },
    )
}
```

- [ ] **Step 4: Verify GREEN and run proportional regression checks**

Run the focused regression test:

```bash
./gradlew :ui:revenuecatui:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.ui.revenuecatui.components.OverlayLayoutAccessibilityTests"
```

Expected: PASS.

Run the complete RevenueCatUI defaults unit-test task:

```bash
./gradlew :ui:revenuecatui:testDefaultsDebugUnitTest
```

Expected: BUILD SUCCESSFUL with all RevenueCatUI defaults tests passing.

Run static analysis:

```bash
./gradlew detektAll
```

Expected: BUILD SUCCESSFUL with no new Detekt findings.

Finally run:

```bash
git diff --check
git status --short
```

Expected: no whitespace errors; only the two production files, the new regression test, and this implementation plan are changed after the earlier design-spec commit.

- [ ] **Step 5: Commit the verified implementation**

```bash
git add \
  ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/LoadedPaywallComponents.kt \
  ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/LoadedWorkflowPaywall.kt \
  ui/revenuecatui/src/test/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/OverlayLayoutAccessibilityTests.kt
git add -f docs/superpowers/plans/2026-08-12-paywall-accessibility-traversal-order.md
git commit -m "fix(paywalls): define accessibility traversal order"
```

Expected: commit succeeds after the repository's pre-commit Detekt check.
