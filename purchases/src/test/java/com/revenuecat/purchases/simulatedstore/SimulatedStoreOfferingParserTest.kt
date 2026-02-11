package com.revenuecat.purchases.simulatedstore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.models.StoreProduct
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class SimulatedStoreOfferingParserTest {

    private lateinit var parser: SimulatedStoreOfferingParser
    private lateinit var mockProduct1: StoreProduct
    private lateinit var mockProduct2: StoreProduct

    @Before
    public fun setup() {
        parser = SimulatedStoreOfferingParser()
        mockProduct1 = mockk(relaxed = true) {
            every { copyWithPresentedOfferingContext(any()) } returns this@mockk
        }
        mockProduct2 = mockk(relaxed = true) {
            every { copyWithPresentedOfferingContext(any()) } returns this@mockk
        }
    }

    @Test
    fun `createPackage returns package when product exists`() {
        val productId = "test_product_id"
        val packageIdentifier = "test_package"
        val productsById = mapOf(
            productId to listOf(mockProduct1, mockProduct2)
        )
        val packageJson = JSONObject().apply {
            put("platform_product_identifier", productId)
            put("identifier", packageIdentifier)
        }
        val presentedOfferingContext = PresentedOfferingContext("test_offering")

        val result = parser.createPackage(packageJson, productsById, presentedOfferingContext)

        assertThat(result).isNotNull
        assertThat(result?.identifier).isEqualTo(packageIdentifier)
        assertThat(result?.product).isEqualTo(mockProduct1)
    }

    @Test
    fun `createPackage returns null when product does not exist`() {
        val productId = "nonexistent_product_id"
        val packageIdentifier = "test_package"
        val productsById = mapOf(
            "other_product_id" to listOf(mockProduct1)
        )
        val packageJson = JSONObject().apply {
            put("platform_product_identifier", productId)
            put("identifier", packageIdentifier)
        }
        val presentedOfferingContext = PresentedOfferingContext("test_offering")

        val result = parser.createPackage(packageJson, productsById, presentedOfferingContext)

        assertThat(result).isNull()
    }

    @Test
    fun `createPackage returns null when productsById is empty`() {
        val productId = "test_product_id"
        val packageIdentifier = "test_package"
        val productsById = emptyMap<String, List<StoreProduct>>()
        val packageJson = JSONObject().apply {
            put("platform_product_identifier", productId)
            put("identifier", packageIdentifier)
        }
        val presentedOfferingContext = PresentedOfferingContext("test_offering")

        val result = parser.createPackage(packageJson, productsById, presentedOfferingContext)

        assertThat(result).isNull()
    }

    @Test
    fun `createPackage returns package with first product when multiple products exist for same id`() {
        val productId = "test_product_id"
        val packageIdentifier = "test_package"
        val productsById = mapOf(
            productId to listOf(mockProduct1, mockProduct2)
        )
        val packageJson = JSONObject().apply {
            put("platform_product_identifier", productId)
            put("identifier", packageIdentifier)
        }
        val presentedOfferingContext = PresentedOfferingContext("test_offering")

        val result = parser.createPackage(packageJson, productsById, presentedOfferingContext)

        assertThat(result).isNotNull
        assertThat(result?.product).isEqualTo(mockProduct1)
    }
}
