import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.sample.ui.theme.CustomEntitlementComputationTheme
import org.json.JSONObject
import java.util.Date

data class CustomerInfoEvent(
    val date: Date,
    val appUserID: String,
    val jsonObject: JSONObject
)

@Composable
fun EventList(events: List<CustomerInfoEvent>, onEventClicked: (CustomerInfoEvent) -> Unit) {
    LazyColumn {
        items(events) { event ->
            EventListItem(event = event, onEventClicked = onEventClicked)
        }
    }
}

@Composable
fun EventListItem(event: CustomerInfoEvent, onEventClicked: (CustomerInfoEvent) -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onEventClicked(event) }
            .padding(16.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("Fired at: ")
                }
                append(event.date.toString())
            }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("App User ID: ")
                }
                append(event.appUserID)
            }
        )
    }
}

@Preview
@Composable
fun EventListItemPreview() {
    CustomEntitlementComputationTheme {
        Surface(color = Color.White) {
            EventListItem(
                event = CustomerInfoEvent(
                    date = Date(),
                    appUserID = "appUserID",
                    jsonObject = JSONObject()
                ),
                onEventClicked = {}
            )
        }
    }
}