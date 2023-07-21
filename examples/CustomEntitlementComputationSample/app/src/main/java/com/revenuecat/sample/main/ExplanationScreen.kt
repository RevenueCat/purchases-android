import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.revenuecat.sample.ui.theme.CustomEntitlementComputationTheme

@Composable
fun ExplanationScreen(onDismiss: () -> Unit) {
    Surface(color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Custom Entitlements Mode",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(8.dp)
            )

            Text(
                text = "This mode is intended for apps that will do their own entitlement " +
                    "computation, separate from RevenueCat.",
                modifier = Modifier.padding(8.dp)
            )

            Text(
                text = "In this mode, RevenueCat will not generate anonymous user IDs, " +
                    "it will not refresh customerInfo cache automatically " +
                    "(only when a purchase goes through), and it will disallow methods " +
                    "other than those for configuration, switching users, " +
                    "getting offerings and purchases.",
                modifier = Modifier.padding(8.dp)
            )

            Text(
                text = "Use switchUser to switch to a different App User ID if needed. " +
                    "The SDK should only be configured once the initial appUserID is known.",
                modifier = Modifier.padding(8.dp)
            )

            Text(
                text = "Apps using this mode rely on webhooks to signal their backends to " +
                    "refresh entitlements with RevenueCat.",
                modifier = Modifier.padding(8.dp)
            )

            Spacer(modifier = Modifier.weight(1f, true))
            Button(
                onClick = onDismiss,
                content = { Text("Close") }
            )
        }
    }

}

@Preview
@Composable
fun ExplanationScreenPreview() {
    CustomEntitlementComputationTheme {
        ExplanationScreen(onDismiss = {})
    }
}