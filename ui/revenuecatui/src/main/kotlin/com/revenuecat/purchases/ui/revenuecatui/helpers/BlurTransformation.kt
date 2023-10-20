import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.VisibleForTesting
import coil.size.Size
import coil.transform.Transformation
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
    private val context: Context,
    private val radius: Float,
) : Transformation {
    override val cacheKey: String = "${javaClass.name}-$radius"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        return input.blur(context, radius)
    }
}

private object BlurConstants {
    // max radius supported by RenderScript
    const val maxSupportedRadius = 25f
    const val maxImageSize = 400
}

@VisibleForTesting
internal fun Bitmap.blur(context: Context, radius: Float, scaleDown: Boolean = true): Bitmap {
    if (radius < 1f) {
        return this@blur
    }
    val updatedRadius = min(radius.toDouble(), BlurConstants.maxSupportedRadius.toDouble())

    val bitmap = if (scaleDown) this.scaledDown() else this

    val rs = RenderScript.create(context)
    val input = Allocation.createFromBitmap(rs, bitmap)
    val output = Allocation.createTyped(rs, input.type)
    val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

    script.setRadius(updatedRadius.toFloat())
    script.setInput(input)
    script.forEach(output)

    val blurredBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, config)
    output.copyTo(blurredBitmap)

    input.destroy()
    output.destroy()
    script.destroy()
    rs.destroy()

    return blurredBitmap
}

private fun Bitmap.scaledDown(): Bitmap {
    val ratio = min(
        BlurConstants.maxImageSize.toFloat() / width,
        BlurConstants.maxImageSize.toFloat() / height,
    )
    val width = (ratio * width).roundToInt()
    val height = (ratio * height).roundToInt()

    return Bitmap.createScaledBitmap(this, width, height, true)
}
