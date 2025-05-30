package com.revenuecat.purchases.paywalls.components

import android.content.Context
import android.content.res.Resources
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.UiConfig.AppConfig.FontsConfig
import com.revenuecat.purchases.paywalls.components.properties.FontSpec
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FontSpecProviderTests {

    private val mockPackageName = "com.revenuecat.purchases.test"

    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources

    private lateinit var fontSpecProvider: FontSpecProvider

    @Before
    fun setUp() {
        mockResources = mockk()
        mockContext = mockk<Context>().apply {
            every { resources } returns mockResources
            every { packageName } returns mockPackageName
        }

        fontSpecProvider = FontSpecProvider(mockContext)
    }

    @Test
    fun `fontSpecProvider loads fonts from offering with fonts`() {
        val font1Name = "Font1"
        val font2Name = "Font2"
        val font1Alias = FontAlias("font1")
        val font2Alias = FontAlias("font2")
        val font3Alias = FontAlias("font3")
        every { mockResources.getIdentifier(font1Name, "font", mockPackageName) } returns 123
        every { mockResources.getIdentifier(font2Name, "font", mockPackageName) } returns 0
        every { mockResources.assets.list("fonts") } returns arrayOf("other_font.ttf", "${font2Name}.ttf")
        val offering = createOfferingWithFonts(mapOf(
            font1Alias to FontsConfig(
                android = FontsConfig.FontInfo.Name(font1Name)
            ),
            font2Alias to FontsConfig(
                android = FontsConfig.FontInfo.Name(font2Name)
            ),
            font3Alias to FontsConfig(
                android = FontsConfig.FontInfo.Name("serif")
            )
        ))
        fontSpecProvider.loadFonts(offering)
        val fontSpecs = fontSpecProvider.getFontSpecMap()
        assertThat(fontSpecs.size).isEqualTo(3)
        assertThat(fontSpecs[font1Alias]).isEqualTo(FontSpec.Resource(123))
        assertThat(fontSpecs[font2Alias]).isEqualTo(FontSpec.Asset("fonts/${font2Name}.ttf"))
        assertThat(fontSpecs[font3Alias]).isEqualTo(FontSpec.Generic.Serif)
    }

    @Test
    fun `fontSpecProvider does not modify already existing fonts, and add new ones`() {
        val font1Name = "Font1"
        val font2Name = "Font2"
        val font1Alias = FontAlias("font1")
        val font2Alias = FontAlias("font2")
        val font3Alias = FontAlias("font3")
        every { mockResources.getIdentifier(font1Name, "font", mockPackageName) } returns 123
        every { mockResources.getIdentifier(font2Name, "font", mockPackageName) } returns 0
        every { mockResources.assets.list("fonts") } returns arrayOf("other_font.ttf", "${font2Name}.ttf")
        val offering1 = createOfferingWithFonts(mapOf(
            font1Alias to FontsConfig(
                android = FontsConfig.FontInfo.Name(font1Name)
            ),
            font2Alias to FontsConfig(
                android = FontsConfig.FontInfo.Name(font2Name)
            )
        ))
        val offering2 = createOfferingWithFonts(mapOf(
            font2Alias to FontsConfig(
                android = FontsConfig.FontInfo.Name(font1Name)
            ),
            font3Alias to FontsConfig(
                android = FontsConfig.FontInfo.Name("serif")
            )
        ))
        fontSpecProvider.loadFonts(offering1)
        val fontSpecsAfterOffering1Load = fontSpecProvider.getFontSpecMap()
        assertThat(fontSpecsAfterOffering1Load.size).isEqualTo(2)
        assertThat(fontSpecsAfterOffering1Load[font1Alias]).isEqualTo(FontSpec.Resource(123))
        assertThat(fontSpecsAfterOffering1Load[font2Alias]).isEqualTo(FontSpec.Asset("fonts/${font2Name}.ttf"))

        fontSpecProvider.loadFonts(offering2)
        val fontSpecsAfterOffering2Load = fontSpecProvider.getFontSpecMap()
        assertThat(fontSpecsAfterOffering2Load.size).isEqualTo(3)
        assertThat(fontSpecsAfterOffering2Load[font1Alias]).isEqualTo(FontSpec.Resource(123))
        assertThat(fontSpecsAfterOffering2Load[font2Alias]).isEqualTo(FontSpec.Asset("fonts/${font2Name}.ttf"))
        assertThat(fontSpecsAfterOffering2Load[font3Alias]).isEqualTo(FontSpec.Generic.Serif)
    }

    @Test
    fun `fontSpecProvider does not load fonts from offering without fonts`() {
        val offering = createOfferingWithFonts(emptyMap())
        fontSpecProvider.loadFonts(offering)
        val fontSpecs = fontSpecProvider.getFontSpecMap()
        assertThat(fontSpecs.size).isEqualTo(0)
    }


    private fun createOfferingWithFonts(fontMap: Map<FontAlias, FontsConfig>) : Offering {
        return Offering(
            identifier = "test_offering",
            serverDescription = "Test Offering",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywall = null,
            paywallComponents = Offering.PaywallComponents(
                uiConfig = UiConfig(
                    app = UiConfig.AppConfig(
                        fonts = fontMap
                    )
                ),
                data = mockk(),
            )
        )
    }
}
