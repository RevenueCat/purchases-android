package com.revenuecat.purchases.ui.revenuecatui.composables

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebCheckoutBottomSheet(
    url: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val sheetHeight = screenHeight / 2
    var isLoading by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(sheetHeight),
        ) {
            WebCheckoutWebView(
                url = url,
                onLoadingChanged = { isLoading = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(sheetHeight),
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

private class NestedScrollWebView(context: Context) : WebView(context) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.onTouchEvent(event)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebCheckoutWebView(
    url: String,
    onLoadingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            NestedScrollWebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(false)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onLoadingChanged(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onLoadingChanged(false)
                    }
                }
                webChromeClient = WebChromeClient()

                loadUrl(url)
            }
        },
        modifier = modifier,
    )
}
