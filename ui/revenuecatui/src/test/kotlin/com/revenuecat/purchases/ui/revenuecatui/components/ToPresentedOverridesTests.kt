package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentConditions
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.ComponentStates
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class ToPresentedOverridesTests(@Suppress("UNUSED_PARAMETER") name: String, private val args: Args) {

    // PartialTextComponent and LocalizedTextPartial are an arbitrary choice. Any PartialComponent and PresentedPartial
    // type would do to test the toPresentedOverrides() logic.
    class Args(
        val availableOverrides: ComponentOverrides<PartialTextComponent>,
        val transform: (PartialTextComponent) -> Result<LocalizedTextPartial>,
        val expected: Result<PresentedOverrides<LocalizedTextPartial>>,
    )

    companion object {
        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            // These use the fontName to discern PartialTextComponents.
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
                        if (partial.fontName == "introOffer") Result.failure(IllegalStateException(partial.fontName))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.failure(IllegalStateException("introOffer"))
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
                        if (partial.fontName == "selected") Result.failure(IllegalStateException(partial.fontName))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.failure(IllegalStateException("selected"))
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
                        if (partial.fontName == "compact") Result.failure(IllegalStateException(partial.fontName))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.failure(IllegalStateException("compact"))
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
                        if (partial.fontName == "medium") Result.failure(IllegalStateException(partial.fontName))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.failure(IllegalStateException("medium"))
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
                        if (partial.fontName == "expanded") Result.failure(IllegalStateException(partial.fontName))
                        else LocalizedTextPartial(from = partial, using = emptyMap())
                    },
                    expected = Result.failure(IllegalStateException("expanded"))
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
                    expected = Result.success(
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
        assertEquals(args.expected.isFailure, actual.isFailure)
        assertEquals(args.expected.exceptionOrNull()?.message, actual.exceptionOrNull()?.message)
        assertEquals(args.expected.isSuccess, actual.isSuccess)
        assertEquals(args.expected.getOrNull(), actual.getOrNull())
    }
}
