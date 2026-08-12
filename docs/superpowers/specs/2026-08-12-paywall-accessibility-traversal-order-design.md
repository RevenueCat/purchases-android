# Paywall Accessibility Traversal Order Design

## Problem

`PaywallComponentsScaffold` emits its main content before its fixed header and sticky footer because
`OverlayLayout` needs the main content at child index 0. The layout then places the header and footer
at their correct visual positions, but visual placement does not change semantics-tree emission order.
Because the full-screen body and header overlap at the top of the screen, Compose cannot determine the
intended TalkBack order from geometry alone and traverses the body before the header.

The shared scaffold is used by standard Paywalls V2 and workflow paywalls, so both variants are
affected.

## Accessibility Contract

When present, the three scaffold regions must be exposed to screen readers in this order:

1. Fixed header
2. Main paywall body
3. Sticky footer

Content inside each region retains its own natural traversal order. Missing optional regions are
skipped without changing the relative order of the remaining regions.

## Design

Keep the existing child emission, measurement, placement, and drawing order unchanged. Mark
`OverlayLayout` as a Compose semantics traversal group, then have it emit each supplied region in its
own peer traversal group with an explicit index:

- Header: `-1f`
- Main content: `0f`
- Sticky footer: `1f`

Compose compares `traversalIndex` values at a peer level and only honors them on focusable nodes or
traversal groups. Region wrappers therefore need to be traversal groups rather than merely carrying a
`traversalIndex` property. Explicit indices make the complete contract deterministic instead of
depending on geometry or child emission order.

Use layout-neutral `Box` wrappers with propagated minimum constraints so each region receives the same
constraints it receives today. Change the internal `OverlayLayout` contract from positional content
plus `hasHeader`/`hasFooter` flags to named `mainContent`, `headerContent`, and `footerContent` lambdas.
This keeps region semantics and child indexing centralized and prevents call sites from supplying
content that disagrees with the flags.

The wrappers and signature remain private implementation details. Update the standard scaffold and
the nested workflow-step overlay to use the named slots. No public API changes are needed, and
workflow paywalls inherit the same ordering behavior.

## Testing

Add `OverlayLayoutAccessibilityTests`, a Compose regression test that renders the shared overlay with
header, body, and footer content. Inspect the unmerged semantics tree and verify that the three peer
region groups carry the literal indices `-1f`, `0f`, and `1f`, and that their parent is a traversal
group. This guards the SDK's declared ordering contract while leaving Compose's accessibility sorting
algorithm to Compose's own tests.

Run the focused test with
`./gradlew :ui:revenuecatui:testDefaultsDebugUnitTest --tests "*.OverlayLayoutAccessibilityTests"`,
followed by `./gradlew :ui:revenuecatui:testDefaultsDebugUnitTest` and `./gradlew detektAll`.

## Non-goals

- Changing visual stacking, sizing, padding, or scrolling behavior.
- Reordering content within the header, body, or footer.
- Adding separate workflow-specific traversal logic.
- Changing the accessibility order of transient leaving headers during workflow animations; those
  headers are rendered inside the main-content transition surface rather than as the scaffold header.
