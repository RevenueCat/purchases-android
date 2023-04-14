import com.revenuecat.purchases.common.networking.MapConverter
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals

//@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4::class)
class JSONArrayBugInstrumentedTest {
    @org.junit.Test
    fun testReproduceIncorrectJSONArray() {
        val inputMap = mapOf(
            "key1" to "value1",
            "key2" to listOf("value2", "value3"),
            "key3" to mapOf("nestedKey" to "nestedValue"),
            "key4" to mapOf("nestedArray" to listOf("value4", "value5")),
        )
        val incorrectJSONObject = JSONObject(inputMap)
        assertEquals(
            incorrectJSONObject
                .toString(), ("""{"key1":"value1","key2":[value2, value3],"key3":{"nestedKey":"nestedValue"},
                    |"key4":{"nestedArray":[value4, value5]}}""".trimMargin())
        )
        val mapConverter = MapConverter()

        val correctJSONObject = mapConverter.convertToJSON(inputMap)
        assertEquals(
            correctJSONObject
                .toString(), ("""{"key1":"value1","key2":["value2","value3"],"key3":{"nestedKey":"nestedValue"},
                    |"key4":{"nestedArray":["value4","value5"]}}""".trimMargin())
        )
    }
}

