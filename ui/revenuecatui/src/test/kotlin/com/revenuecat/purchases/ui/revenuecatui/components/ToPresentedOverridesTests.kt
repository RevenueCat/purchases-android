package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentConditions
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.ComponentStates
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.InvalidTemplate
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.errorOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrNull
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.isError
import com.revenuecat.purchases.ui.revenuecatui.helpers.isSuccess
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ToPresentedOverridesTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    // PartialTextComponent and LocalizedTextPartial are an arbitrary choice. Any PartialComponent and PresentedPartial
    // type would do to test the toPresentedOverrides() logic.
    // We're using the InvalidTemplate error as a way to pass strings around, as it has a templateName string property.
    class Args(
        val availableOverrides: ComponentOverrides<PartialTextComponent>,
        val transform: (PartialTextComponent) -> Result<LocalizedTextPartial, PaywallValidationError>,
        val expected: Result<PresentedOverrides<LocalizedTextPartial>, InvalidTemplate>,
    )

    companion object {
        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            // These use the fontName to discern PartialTextComponents. The failing one will return an InvalidTemplate
            // error with the fontName as templateName. The use of InvalidTemplate is arbitrary, and chosen because it
            // has a string property we can use to match the actual and expected error.
            arrayOf(
                "Should fail if transforming introOffer fails",
                Args(
                    availableOverrides = ComponentOverrides(
                        introOffer = PartialTextComponent(fontName = "introOffer"),
                        states = ComponentStates(
                            selected = PartialTextComponent(fontName = "selected"),
                        ),
                        conditions = ComponentConditions(
                            compact = PartialTextComponent(fontName = "compact"),
                            medium = PartialTextComponent(fontName = "medium"),
                            expanded = PartialTextComponent(fontName = "expanded"),
                        ),
                    ),
                    transform = { partial ->
                        if (partial.fontName == "introOffer") Result.Error(InvalidTemplate(partial.fontName!!))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.Error(InvalidTemplate("introOffer"))
                )
            ),
            arrayOf(
                "Should fail if transforming selected fails",
                Args(
                    availableOverrides = ComponentOverrides(
                        introOffer = PartialTextComponent(fontName = "introOffer"),
                        states = ComponentStates(
                            selected = PartialTextComponent(fontName = "selected"),
                        ),
                        conditions = ComponentConditions(
                            compact = PartialTextComponent(fontName = "compact"),
                            medium = PartialTextComponent(fontName = "medium"),
                            expanded = PartialTextComponent(fontName = "expanded"),
                        ),
                    ),
                    transform = { partial ->
                        if (partial.fontName == "selected") Result.Error(InvalidTemplate(partial.fontName!!))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.Error(InvalidTemplate("selected"))
                )
            ),
            arrayOf(
                "Should fail if transforming compact fails",
                Args(
                    availableOverrides = ComponentOverrides(
                        introOffer = PartialTextComponent(fontName = "introOffer"),
                        states = ComponentStates(
                            selected = PartialTextComponent(fontName = "selected"),
                        ),
                        conditions = ComponentConditions(
                            compact = PartialTextComponent(fontName = "compact"),
                            medium = PartialTextComponent(fontName = "medium"),
                            expanded = PartialTextComponent(fontName = "expanded"),
                        ),
                    ),
                    transform = { partial ->
                        if (partial.fontName == "compact") Result.Error(InvalidTemplate(partial.fontName!!))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.Error(InvalidTemplate("compact"))
                )
            ),
            arrayOf(
                "Should fail if transforming medium fails",
                Args(
                    availableOverrides = ComponentOverrides(
                        introOffer = PartialTextComponent(fontName = "introOffer"),
                        states = ComponentStates(
                            selected = PartialTextComponent(fontName = "selected"),
                        ),
                        conditions = ComponentConditions(
                            compact = PartialTextComponent(fontName = "compact"),
                            medium = PartialTextComponent(fontName = "medium"),
                            expanded = PartialTextComponent(fontName = "expanded"),
                        ),
                    ),
                    transform = { partial ->
                        if (partial.fontName == "medium") Result.Error(InvalidTemplate(partial.fontName!!))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.Error(InvalidTemplate("medium"))
                )
            ),
            arrayOf(
                "Should fail if transforming expanded fails",
                Args(
                    availableOverrides = ComponentOverrides(
                        introOffer = PartialTextComponent(fontName = "introOffer"),
                        states = ComponentStates(
                            selected = PartialTextComponent(fontName = "selected"),
                        ),
                        conditions = ComponentConditions(
                            compact = PartialTextComponent(fontName = "compact"),
                            medium = PartialTextComponent(fontName = "medium"),
                            expanded = PartialTextComponent(fontName = "expanded"),
                        ),
                    ),
                    transform = { partial ->
                        if (partial.fontName == "expanded") Result.Error(InvalidTemplate(partial.fontName!!))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.Error(InvalidTemplate("expanded"))
                )
            ),
            arrayOf(
                "Should succeed if all transformations succeed",
                Args(
                    availableOverrides = ComponentOverrides(
                        introOffer = PartialTextComponent(fontName = "introOffer"),
                        states = ComponentStates(
                            selected = PartialTextComponent(fontName = "selected"),
                        ),
                        conditions = ComponentConditions(
                            compact = PartialTextComponent(fontName = "compact"),
                            medium = PartialTextComponent(fontName = "medium"),
                            expanded = PartialTextComponent(fontName = "expanded"),
                        ),
                    ),
                    transform = { partial -> LocalizedTextPartial(from = partial, using = emptyMap()) },
                    expected = Result.Success(
                        PresentedOverrides(
                            introOffer = LocalizedTextPartial(
                                from = PartialTextComponent(fontName = "introOffer"),
                                using = emptyMap()
                            ).getOrThrow(),
                            states = PresentedStates(
                                selected = LocalizedTextPartial(
                                    from = PartialTextComponent(fontName = "selected"),
                                    using = emptyMap()
                                ).getOrThrow(),
                            ),
                            conditions = PresentedConditions(
                                compact = LocalizedTextPartial(
                                    from = PartialTextComponent(fontName = "compact"),
                                    using = emptyMap()
                                ).getOrThrow(),
                                medium = LocalizedTextPartial(
                                    from = PartialTextComponent(fontName = "medium"),
                                    using = emptyMap()
                                ).getOrThrow(),
                                expanded = LocalizedTextPartial(
                                    from = PartialTextComponent(fontName = "expanded"),
                                    using = emptyMap()
                                ).getOrThrow(),
                            ),
                        )
                    )
                )
            ),
        )
    }

    @Test
    fun `Should transform expectedly`() {
        // Arrange, Act
        val actual = args.availableOverrides.toPresentedOverrides(args.transform)

        // Assert
        assertEquals(args.expected.isError, actual.isError)
        assertEquals(
            args.expected.errorOrNull()?.templateName,
            (actual.errorOrNull() as? InvalidTemplate)?.templateName
        )
        assertEquals(args.expected.isSuccess, actual.isSuccess)
        assertEquals(args.expected.getOrNull(), actual.getOrNull())
    }
}
