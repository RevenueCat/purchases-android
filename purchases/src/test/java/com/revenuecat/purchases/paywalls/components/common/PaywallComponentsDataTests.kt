package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.JsonTools
import com.revenuecat.purchases.models.Checksum
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeVideoUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VideoUrls
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.net.URL

@RunWith(Parameterized::class)
internal class PaywallComponentsDataTests(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val args: Args,
) {

    class Args(
        @Language("json")
        val json: String,
        val expected: PaywallComponentsData,
    )

    companion object {

        @Suppress("LongMethod")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Collection<*> = listOf(
            arrayOf(
                "revision present",
                Args(
                    json = """
                        {
                          "id": "paywall_id",
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "badge": {
                                  "stack": {
                                    "type": "stack",
                                    "components": []
                                  },
                                  "style": "overlay",
                                  "alignment": "bottom_trailing"
                                },
                                "components": []
                              },
                              "background": {
                                "type": "color",
                                "value": {
                                  "light": {
                                    "type": "alias",
                                    "value": "primary"
                                  }
                                }
                              }
                            }
                          },
                          "components_localizations": {
                            "en_US": {
                              "ZvS4Ck5hGM": "Hello"
                            }
                          },
                          "default_locale": "en_US",
                          "revision": 123
                        }

                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList(),
                                    badge = Badge(
                                        stack = StackComponent(
                                            components = emptyList()
                                        ),
                                        style = Badge.Style.Overlay,
                                        alignment = TwoDimensionalAlignment.BOTTOM_TRAILING
                                    ),
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 123
                    )
                ),
            ),
            arrayOf(
                "revision absent",
                Args(
                    json = """
                        {
                          "id": "paywall_id",
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "components": []
                              },
                              "background": {
                                "type": "color",
                                "value": {
                                  "light": {
                                    "type": "alias",
                                    "value": "primary"
                                  }
                                }
                              }
                            }
                          },
                          "components_localizations": {
                            "en_US": {
                              "ZvS4Ck5hGM": "Hello"
                            }
                          },
                          "default_locale": "en_US"
                        }

                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 0
                    )
                ),
            ),
            arrayOf(
                "id absent",
                Args(
                    json = """
                        {
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "revision": 5,
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "components": []
                              },
                              "background": {
                                "type": "color",
                                "value": {
                                  "light": {
                                    "type": "alias",
                                    "value": "primary"
                                  }
                                }
                              }
                            }
                          },
                          "components_localizations": {
                            "en_US": {
                              "ZvS4Ck5hGM": "Hello"
                            }
                          },
                          "default_locale": "en_US"
                        }

                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        id = null,
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 5
                    )
                ),
            ),
            arrayOf(
                "with zero_decimal_place_countries",
                Args(
                    json = """
                {
                  "id": "paywall_id",
                  "template_name": "components",
                  "asset_base_url": "https://assets.pawwalls.com",
                  "components_config": {
                    "base": {
                      "stack": {
                        "type": "stack",
                        "components": []
                      },
                      "background": {
                        "type": "color",
                        "value": {
                          "light": {
                            "type": "alias",
                            "value": "primary"
                          }
                        }
                      }
                    }
                  },
                  "components_localizations": {
                    "en_US": {
                      "ZvS4Ck5hGM": "Hello"
                    }
                  },
                  "default_locale": "en_US",
                  "zero_decimal_place_countries": {
                    "apple": [
                      "TWN",
                      "KAZ",
                      "MEX",
                      "PHL",
                      "THA"
                    ],
                    "google": [
                      "TW",
                      "KZ",
                      "MX",
                      "PH",
                      "TH"
                    ]
                  }
                }""".trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 0,
                        zeroDecimalPlaceCountries = listOf(
                            "TW",
                            "KZ",
                            "MX",
                            "PH",
                            "TH",
                        ),
                    )
                ),
            ),
            arrayOf(
                "with exit_offers",
                Args(
                    json = """
                {
                  "id": "paywall_id",
                  "template_name": "components",
                  "asset_base_url": "https://assets.pawwalls.com",
                  "components_config": {
                    "base": {
                      "stack": {
                        "type": "stack",
                        "components": []
                      },
                      "background": {
                        "type": "color",
                        "value": {
                          "light": {
                            "type": "alias",
                            "value": "primary"
                          }
                        }
                      }
                    }
                  },
                  "components_localizations": {
                    "en_US": {
                      "ZvS4Ck5hGM": "Hello"
                    }
                  },
                  "default_locale": "en_US",
                  "exit_offers": {
                    "dismiss": {
                      "offering_id": "exit-offering-id"
                    }
                  }
                }""".trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList()
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 0,
                        exitOffers = ExitOffers(
                            dismiss = ExitOffer(
                                offeringId = "exit-offering-id",
                            ),
                        ),
                    )
                ),
            ),
            arrayOf(
                "automatically_scale_font_size present",
                Args(
                    json = """
                        {
                          "id": "paywall_id",
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "badge": {
                                  "stack": {
                                    "type": "stack",
                                    "components": []
                                  },
                                  "style": "overlay",
                                  "alignment": "bottom_trailing"
                                },
                                "components": []
                              },
                              "background": {
                                "type": "color",
                                "value": {
                                  "light": {
                                    "type": "alias",
                                    "value": "primary"
                                  }
                                }
                              }
                            }
                          },
                          "components_localizations": {
                            "en_US": {
                              "ZvS4Ck5hGM": "Hello"
                            }
                          },
                          "default_locale": "en_US",
                          "revision": 123,
                          "automatically_scale_font_size": false
                        }

                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList(),
                                    badge = Badge(
                                        stack = StackComponent(
                                            components = emptyList()
                                        ),
                                        style = Badge.Style.Overlay,
                                        alignment = TwoDimensionalAlignment.BOTTOM_TRAILING
                                    ),
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 123,
                        automaticallyScaleFontSize = false,
                    )
                ),
            ),
            arrayOf(
                "automatically_scale_font_size absent",
                Args(
                    json = """
                        {
                          "id": "paywall_id",
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "badge": {
                                  "stack": {
                                    "type": "stack",
                                    "components": []
                                  },
                                  "style": "overlay",
                                  "alignment": "bottom_trailing"
                                },
                                "components": []
                              },
                              "background": {
                                "type": "color",
                                "value": {
                                  "light": {
                                    "type": "alias",
                                    "value": "primary"
                                  }
                                }
                              }
                            }
                          },
                          "components_localizations": {
                            "en_US": {
                              "ZvS4Ck5hGM": "Hello"
                            }
                          },
                          "default_locale": "en_US",
                          "revision": 123
                        }

                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(
                                    components = emptyList(),
                                    badge = Badge(
                                        stack = StackComponent(
                                            components = emptyList()
                                        ),
                                        style = Badge.Style.Overlay,
                                        alignment = TwoDimensionalAlignment.BOTTOM_TRAILING
                                    ),
                                ),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("ZvS4Ck5hGM") to LocalizationData.Text("Hello")
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                        revision = 123,
                        automaticallyScaleFontSize = true,
                    )
                ),
            ),
            arrayOf(
                "text and image localizations, with video localizations",
                Args(
                    json = """
                        {
                          "id": "paywall_id",
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "components": []
                              },
                              "background": {
                                "type": "color",
                                "value": {
                                  "light": {
                                    "type": "alias",
                                    "value": "primary"
                                  }
                                }
                              }
                            }
                          },
                          "components_video_localizations": {
                            "es_ES": {
                              "video_lid": {
                                "light": {
                                  "url": "https://video.pawwalls.com/es.mp4",
                                  "url_low_res": "https://video.pawwalls.com/es_low_res.mp4",
                                  "checksum": { "algo": "sha256", "value": "abc" },
                                  "width": 1080,
                                  "height": 1920
                                },
                                "dark": {
                                  "url": "https://video.pawwalls.com/es_dark.mp4",
                                  "width": 720,
                                  "height": 1280
                                }
                              }
                            },
                            "en_US": {
                              "video_lid": {
                                "light": {
                                  "url": "https://video.pawwalls.com/en.mp4",
                                  "width": 1080,
                                  "height": 1920
                                }
                              }
                            }
                          },
                          "components_localizations": {
                            "es_ES": {
                              "text_lid": "Hola",
                              "image_lid": {
                                "light": {
                                  "heic": "https://assets.pawwalls.com/es.heic",
                                  "heic_low_res": "https://assets.pawwalls.com/es_low_res.heic",
                                  "original": "https://assets.pawwalls.com/es.png",
                                  "webp": "https://assets.pawwalls.com/es.webp",
                                  "webp_low_res": "https://assets.pawwalls.com/es_low_res.webp",
                                  "width": 100,
                                  "height": 200
                                }
                              }
                            }
                          },
                          "default_locale": "es_ES",
                          "revision": 123
                        }

                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        id = "paywall_id",
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(components = emptyList()),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("es_ES") to mapOf(
                                LocalizationKey("text_lid") to LocalizationData.Text("Hola"),
                                LocalizationKey("image_lid") to LocalizationData.Image(
                                    ThemeImageUrls(
                                        light = ImageUrls(
                                            original = URL("https://assets.pawwalls.com/es.png"),
                                            webp = URL("https://assets.pawwalls.com/es.webp"),
                                            webpLowRes = URL("https://assets.pawwalls.com/es_low_res.webp"),
                                            width = 100.toUInt(),
                                            height = 200.toUInt(),
                                        ),
                                    )
                                ),
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("es_ES"),
                        revision = 123,
                        componentsVideoLocalizations = mapOf(
                            LocaleId("es_ES") to mapOf(
                                LocalizationKey("video_lid") to ThemeVideoUrls(
                                    light = VideoUrls(
                                        width = 1080.toUInt(),
                                        height = 1920.toUInt(),
                                        url = URL("https://video.pawwalls.com/es.mp4"),
                                        checksum = Checksum(Checksum.Algorithm.SHA256, "abc"),
                                        urlLowRes = URL("https://video.pawwalls.com/es_low_res.mp4"),
                                    ),
                                    dark = VideoUrls(
                                        width = 720.toUInt(),
                                        height = 1280.toUInt(),
                                        url = URL("https://video.pawwalls.com/es_dark.mp4"),
                                    ),
                                ),
                            ),
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("video_lid") to ThemeVideoUrls(
                                    light = VideoUrls(
                                        width = 1080.toUInt(),
                                        height = 1920.toUInt(),
                                        url = URL("https://video.pawwalls.com/en.mp4"),
                                    ),
                                    dark = null,
                                ),
                            ),
                        ),
                    )
                ),
            ),
            arrayOf(
                "null video localizations",
                Args(
                    json = """
                        {
                          "template_name": "components",
                          "asset_base_url": "https://assets.pawwalls.com",
                          "components_config": {
                            "base": {
                              "stack": {
                                "type": "stack",
                                "components": []
                              },
                              "background": {
                                "type": "color",
                                "value": {
                                  "light": {
                                    "type": "alias",
                                    "value": "primary"
                                  }
                                }
                              }
                            }
                          },
                          "components_localizations": {
                            "en_US": {
                              "text_lid": "Hello"
                            }
                          },
                          "components_video_localizations": null,
                          "default_locale": "en_US"
                        }
                        """.trimIndent(),
                    expected = PaywallComponentsData(
                        templateName = "components",
                        assetBaseURL = URL("https://assets.pawwalls.com"),
                        componentsConfig = ComponentsConfig(
                            base = PaywallComponentsConfig(
                                stack = StackComponent(components = emptyList()),
                                background = Background.Color(
                                    value = ColorScheme(
                                        light = ColorInfo.Alias(ColorAlias("primary"))
                                    )
                                )
                            )
                        ),
                        componentsLocalizations = mapOf(
                            LocaleId("en_US") to mapOf(
                                LocalizationKey("text_lid") to LocalizationData.Text("Hello"),
                            )
                        ),
                        defaultLocaleIdentifier = LocaleId("en_US"),
                    )
                ),
            ),
        )
    }

    @Test
    fun `Should properly deserialize PaywallComponentsData`() {
        // Arrange, Act
        val actual = JsonTools.json.decodeFromString<PaywallComponentsData>(args.json)

        // Assert
        assert(actual == args.expected)
    }
}
