import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.min

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

// max radius supported by RenderScript
private const val MAX_SUPPORTED_RADIUS = 25f

internal fun Bitmap.blur(context: Context, radius: Float = MAX_SUPPORTED_RADIUS): Bitmap {
    if (radius < 1f) {
        return this@blur
    }
    val updatedRadius = min(radius.toDouble(), MAX_SUPPORTED_RADIUS.toDouble())

    val rs = RenderScript.create(context)
    val input = Allocation.createFromBitmap(rs, this)
    val output = Allocation.createTyped(rs, input.type)
    val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

    script.setRadius(updatedRadius.toFloat())
    script.setInput(input)
    script.forEach(output)

    val blurredBitmap = Bitmap.createBitmap(width, height, config)
    output.copyTo(blurredBitmap)

    input.destroy()
    output.destroy()
    script.destroy()
    rs.destroy()

    return blurredBitmap
}
