# iOS Missing Text Localization Parity

## Goal

Match the iOS Paywalls V2 behavior when a base `TextComponent` references a string localization that is missing from one or more locale dictionaries. Android should continue rendering the configured components paywall and use an empty string for each missing value instead of displaying the fallback paywall.

For example, if `de_DE` contains the title but not the subtitle, the German paywall should render with the translated title and a blank subtitle. It should not substitute the subtitle from `default_locale`.

## Scope

This behavior applies only to the base text of a `TextComponent`.

The existing strict validation remains unchanged for:

- Text referenced only by a conditional component override.
- Navigation and checkout URLs stored as localized strings.
- Localized image and video overrides.
- Missing default localization dictionaries and other structural validation failures.

This boundary matches iOS, where a failed lookup for a base text localization becomes an empty string while localized data used by throwing construction paths remains strict.

## Design

Add a base-text lookup in the localization helpers that builds a non-empty map containing every supported locale. For each locale, it returns the localized text when the referenced value exists and is a string. Otherwise it returns an empty string and logs a warning identifying the localization key and locale.

`StyleFactory.createTextComponentStyle` will use this tolerant lookup for `TextComponent.text`. Its override conversion will continue using the existing strict `stringForAllLocales` lookup. All other callers of `stringForAllLocales`, `imageForAllLocales`, and `videoForAllLocales` remain unchanged.

This preserves Android's current ability to precompute styles for all supported locales without changing the runtime locale-switching architecture.

## Error Handling

A missing base text is recoverable and will not produce a `PaywallValidationError`. The warning provides diagnostic visibility without causing the offering mapper to replace the components paywall.

Missing localized values outside this narrow path continue producing their existing validation errors and fallback behavior.

## Testing

Tests will demonstrate that:

1. A base text missing from one secondary locale produces a successful `TextComponentStyle`.
2. Existing translations are preserved and the missing locale receives an empty string.
3. A paywall containing partially localized base texts validates as a components paywall instead of a legacy fallback.
4. Missing override text remains a validation error, guarding the intended scope.

Implementation will follow test-driven development: each changed behavior test must fail under the current implementation before production code is changed.
