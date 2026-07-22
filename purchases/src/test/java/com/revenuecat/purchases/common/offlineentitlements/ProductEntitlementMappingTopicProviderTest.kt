package com.revenuecat.purchases.common.offlineentitlements

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigBlobData
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigManager
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigReadState
import com.revenuecat.purchases.common.remoteconfig.RemoteConfigTopic
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
internal class ProductEntitlementMappingTopicProviderTest {

    private lateinit var manager: RemoteConfigManager
    private lateinit var provider: ProductEntitlementMappingTopicProvider

    @Before
    fun setUp() {
        manager = mockk()
        provider = ProductEntitlementMappingTopicProvider(manager)
    }

    @Test
    fun `getProductEntitlementMapping decodes the default blob`() = runTest {
        val blob = """
            {
              "product_entitlement_mapping": {
                "monthly": {
                  "product_identifier": "monthly",
                  "base_plan_id": "monthly-base-plan",
                  "entitlements": ["pro"]
                }
              }
            }
        """.trimIndent().toByteArray()
        stubBlobRead(blob)

        val result = provider.getProductEntitlementMapping()

        assertThat(result?.mapping?.mappings).containsOnlyKeys("monthly")
        assertThat(result?.mapping?.mappings?.get("monthly")).isEqualTo(
            ProductEntitlementMapping.Mapping(
                productIdentifier = "monthly",
                basePlanId = "monthly-base-plan",
                entitlements = listOf("pro"),
            ),
        )
        coVerify(exactly = 1) {
            manager.blobDataSnapshot(
                RemoteConfigTopic.ProductEntitlementMapping,
                "default",
                any<(ByteArray) -> ProductEntitlementMapping?>(),
            )
        }
    }

    @Test
    fun `getProductEntitlementMapping returns null when the default blob is unavailable`() = runTest {
        coEvery {
            manager.blobDataSnapshot(
                RemoteConfigTopic.ProductEntitlementMapping,
                "default",
                any<(ByteArray) -> ProductEntitlementMapping?>(),
            )
        } returns null

        assertThat(provider.getProductEntitlementMapping()).isNull()
    }

    @Test
    fun `getProductEntitlementMapping returns null when the default blob is malformed`() = runTest {
        stubBlobRead("{ malformed".toByteArray())

        assertThat(provider.getProductEntitlementMapping()).isNull()
    }

    @Test
    fun `getProductEntitlementMapping result is rejected when config changes before use`() = runTest {
        stubBlobRead(
            """
                {
                  "product_entitlement_mapping": {
                    "monthly": {
                      "product_identifier": "monthly",
                      "entitlements": ["pro"]
                    }
                  }
                }
            """.trimIndent().toByteArray(),
        )

        every { manager.useIfCurrent(any<RemoteConfigBlobData<ProductEntitlementMapping>>(), any()) } returns false

        val result = provider.getProductEntitlementMapping()

        assertThat(result?.cacheIfCurrent {}).isFalse()
    }

    private fun stubBlobRead(blob: ByteArray) {
        coEvery {
            manager.blobDataSnapshot(
                RemoteConfigTopic.ProductEntitlementMapping,
                "default",
                any<(ByteArray) -> ProductEntitlementMapping?>(),
            )
        } answers {
            val value = thirdArg<(ByteArray) -> ProductEntitlementMapping?>().invoke(blob)
            value?.let {
                RemoteConfigBlobData(
                    it,
                    RemoteConfigTopic.ProductEntitlementMapping,
                    "default",
                    RemoteConfigReadState(0, 0, "ref"),
                )
            }
        }
    }
}
