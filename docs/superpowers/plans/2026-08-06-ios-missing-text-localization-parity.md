# iOS Missing Text Localization Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render a components paywall with an empty string when a base `TextComponent` localization is missing, matching iOS behavior.

**Architecture:** Keep Android's all-locales style precomputation. Add one tolerant localization helper for base text, use it only from `StyleFactory.createTextComponentStyle`, and leave strict lookup paths for overrides, URLs, images, and videos unchanged.

**Tech Stack:** Kotlin, JUnit 4, Robolectric, AssertJ, Gradle

## Global Constraints

- Missing base text becomes `""`; it does not fall back to `default_locale`.
- Existing translations remain unchanged.
- Missing conditional override text, URLs, images, and videos remain validation errors.
- Follow test-driven development and observe the focused tests fail before modifying production code.

---

### Task 1: Recover Missing Base Text Localizations

**Files:**
- Modify: `ui/revenuecatui/src/test/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/style/StyleFactoryTests.kt`
- Modify: `ui/revenuecatui/src/test/kotlin/com/revenuecat/purchases/ui/revenuecatui/PaywallComponentDataValidationTests.kt`
- Modify: `ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/ktx/Localization.kt`
- Modify: `ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/style/StyleFactory.kt`

**Interfaces:**
- Consumes: `LocalizationDictionary.string(LocalizationKey)` and `NonEmptyMap.mapValues`.
- Produces: `NonEmptyMap<LocaleId, LocalizationDictionary>.stringForAllLocalesOrEmpty(LocalizationKey): NonEmptyMap<LocaleId, String>`.

- [ ] **Step 1: Change the StyleFactory test to require empty text for a missing locale**

Replace `Should fail to create a TextComponentStyle if localized text is missing` with a test named `Should use empty string if localized base text is missing`. Keep the existing two-locale fixture, then assert:

```kotlin
assertThat(result.isSuccess).isTrue()
val style = (result as Result.Success).value.componentStyle as TextComponentStyle
assertThat(style.texts[defaultLocale]).isEqualTo(expectedText)
assertThat(style.texts[otherLocale]).isEmpty()
```

The adjacent `Should fail to create a TextComponentStyle if localized text is missing from an override` test remains unchanged and protects the strict override path.

- [ ] **Step 2: Change the paywall validation regression test to require components rendering**

Rename `Should accumulate errors with Legacy fallback if some localizations are missing` to `Should use empty strings if some base text localizations are missing`. Keep the fixture, then assert the real validated styles:

```kotlin
assertTrue(validated is PaywallValidationResult.Components)
val stack = validated.stack as StackComponentStyle
val firstText = stack.children[0] as TextComponentStyle
val secondText = stack.children[1] as TextComponentStyle
assertEquals("", firstText.texts.getValue(LocaleId("es_ES")))
assertEquals("", secondText.texts.getValue(LocaleId("nl_NL")))
assertEquals("value1", firstText.texts.getValue(defaultLocale))
assertEquals("value2", secondText.texts.getValue(defaultLocale))
```

Remove the now-unused `MissingStringLocalization` import from this test file.

- [ ] **Step 3: Run both test classes and confirm the new expectations fail**

Run:

```bash
./gradlew :ui:revenuecatui:testDefaultsDebugUnitTest \
  --tests "com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactoryTests" \
  --tests "com.revenuecat.purchases.ui.revenuecatui.PaywallComponentDataValidationTests"
```

Expected: the two changed tests fail because `StyleFactory` returns `Result.Error` and paywall validation returns `PaywallValidationResult.Legacy`. The unchanged missing-override test passes.

- [ ] **Step 4: Add the tolerant base-text localization helper**

In `Localization.kt`, import `Logger` and `getOrElse`, then add:

```kotlin
@JvmSynthetic
internal fun NonEmptyMap<LocaleId, LocalizationDictionary>.stringForAllLocalesOrEmpty(
    key: LocalizationKey,
): NonEmptyMap<LocaleId, String> =
    mapValues { (locale, localizationDictionary) ->
        localizationDictionary.string(key).getOrElse {
            Logger.w(MissingStringLocalization(key, locale).message)
            ""
        }
    }
```

This retains each supported locale in the result, preserves valid text, and emits the existing locale-specific diagnostic when recovery is needed.

- [ ] **Step 5: Use the tolerant helper only for base TextComponent text**

In `StyleFactory.kt`, import `stringForAllLocalesOrEmpty`. Replace the current `first = localizations.stringForAllLocales(component.text).flatMapError { ... }` branch in `createTextComponentStyle` with:

```kotlin
first = Result.Success(localizations.stringForAllLocalesOrEmpty(component.text)),
```

Remove the unused `flatMapError` import. Do not change `LocalizedTextPartial`, URL actions, image lookup, or video lookup.

- [ ] **Step 6: Run the focused tests and confirm they pass**

Run the same Gradle command from Step 3.

Expected: both test classes pass, including the unchanged strict override test.

- [ ] **Step 7: Run RevenueCatUI unit tests and static checks**

Run:

```bash
./gradlew :ui:revenuecatui:testDefaultsDebugUnitTest
./gradlew :ui:revenuecatui:detekt
```

Expected: both commands succeed without new failures.

- [ ] **Step 8: Commit the implementation**

```bash
git add \
  ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/ktx/Localization.kt \
  ui/revenuecatui/src/main/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/style/StyleFactory.kt \
  ui/revenuecatui/src/test/kotlin/com/revenuecat/purchases/ui/revenuecatui/components/style/StyleFactoryTests.kt \
  ui/revenuecatui/src/test/kotlin/com/revenuecat/purchases/ui/revenuecatui/PaywallComponentDataValidationTests.kt
git commit -m "fix: tolerate missing base text localizations"
```
