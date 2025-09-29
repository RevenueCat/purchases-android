import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.scale
import coil.size.Size
import coil.transform.Transformation
import com.revenuecat.purchases.ui.revenuecatui.helpers.RenderScriptToolkit
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * BlurTransformation class applies a blur on a given Bitmap image.
 * The blurring is performed with RenderScript, and should only be used in API level < 31, since newer versions
 * of Android have their own native blur implementation.
 *
 * @property radius - The radius of the square used for blurring. Higher values
 *                    produce a more pronounced blur effect.
 */
internal class BlurTransformation(
    private val radius: Int,
) : Transformation {
    override val cacheKey: String = "${javaClass.name}-$radius"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        return input.blur(radius)
    }
}

private object BlurConstants {
    // max radius supported by RenderScript
    const val maxSupportedRadius = 25
    const val maxImageSize = 400
}

@VisibleForTesting
internal fun Bitmap.blur(radius: Int, scaleDown: Boolean = true): Bitmap {
    if (radius < 1f) {
        return this@blur
    }
    val updatedRadius = min(radius, BlurConstants.maxSupportedRadius)
    val bitmap = if (scaleDown) this.scaledDown() else this
    val blurredBitmap = RenderScriptToolkit.blur(inputBitmap = bitmap, radius = updatedRadius)
    return blurredBitmap ?: bitmap
}

private fun Bitmap.scaledDown(): Bitmap {
    val ratio = min(
        BlurConstants.maxImageSize.toFloat() / width,
        BlurConstants.maxImageSize.toFloat() / height,
    )
    val width = (ratio * width).roundToInt()
    val height = (ratio * height).roundToInt()

    return this.scale(width, height)
}
