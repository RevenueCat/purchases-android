import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import coil.size.Size
import coil.transform.Transformation

/**
 * BlurTransformation class applies a Box Blur algorithm on a given Bitmap image.
 * The blurring is performed by averaging the color values of a square group of
 * adjacent pixels for each pixel in the image.
 *
 * @property radius - The radius of the square used for blurring. Higher values
 *                    produce a more pronounced blur effect.
 * @property scale  - The scale factor for resizing the image before blurring.
 *                    Lower values produce a faster but lower quality blur.
 */
class BlurTransformation(
    private val context: Context,
    private val radius: Float = 25f,
    private val scale: Float = 0.5f,
) : Transformation {

    override val cacheKey: String = "${javaClass.name}-$radius"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        return input.blur(context, radius) ?: input
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is BlurTransformation && radius == other.radius
    }

    override fun hashCode(): Int = radius.hashCode()
}

fun Bitmap.blur(context: Context, radius: Float = 25f): Bitmap? {
    return if (Build.VERSION.SDK_INT >= 31) {
        this.blur(context = context, radius = radius)
    } else {
        blurUsingRenderScript(context, radius)
    }
}

private fun Bitmap.blurUsingRenderScript(context: Context, radius: Float): Bitmap? {

    if (radius !in 0.0f..25.0f) {
        throw IllegalArgumentException("Radius must be between 0 and 25.")
    }

    val rs = RenderScript.create(context)
    val input = Allocation.createFromBitmap(rs, this)
    val output = Allocation.createTyped(rs, input.type)
    val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

    script.setRadius(radius)
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