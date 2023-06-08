package com.revenuecat.sample.data

import com.revenuecat.sample.R
import kotlin.random.Random

class SampleWeatherData(val emoji: String, val temperature: String) {
    enum class TemperatureUnit(val unit: String) {
        F("f"), C("c")
    }

    enum class Environment(val planet: String) {
        MERCURY("mercury"),
        VENUS("venus"),
        EARTH("earth"),
        MARS("mars"),
        JUPITER("jupiter"),
        SATURN("saturn"),
        URANUS("uranus"),
        NEPTUNE("neptune"),

        /*
         Pluto is still a planet in my ❤️
         */
        PLUTO("pluto"),
    }

    var unit: TemperatureUnit = TemperatureUnit.F
    var environment: Environment = Environment.EARTH

    val weatherColor: Int
        get() {
            return when (emoji) {
                in "🥶" -> R.color.really_cold
                in "❄️" -> R.color.cold
                in "☁️" -> R.color.cloudy
                in "🌤" -> R.color.sunny
                in "🥵" -> R.color.hot
                in "☄️" -> R.color.really_hot
                else -> R.color.cold
            }
        }

    companion object {
        val testCold = SampleWeatherData("❄️", "14")
        val testHot = SampleWeatherData("☀️", "85")

        fun generateSampleData(environment: Environment, temperature: Int?): SampleWeatherData {
            val temp: Int = temperature ?: Random.nextInt(-20, 120)

            val emoji: String
            when (temp) {
                in 0..32 -> {
                    emoji = "❄️"
                }
                in 33..60 -> {
                    emoji = "☁️"
                }
                in 61..90 -> {
                    emoji = "🌤"
                }
                in 91..120 -> {
                    emoji = "🥵"
                }
                else -> {
                    emoji = if (temp < 0) {
                        "🥶"
                    } else {
                        "☄️"
                    }
                }
            }

            return SampleWeatherData(emoji, temp.toString())
        }
    }
}
