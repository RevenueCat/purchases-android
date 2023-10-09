import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

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
    private val radius: Int = 25,
    private val scale: Float = 0.5f,
) : Transformation {

    override val cacheKey: String = "${javaClass.name}-$radius"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap = input.blur(scale, radius) ?: input

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is BlurTransformation && radius == other.radius
    }

    override fun hashCode(): Int = radius.hashCode()
}

/**
 * Applies blur effect to a Bitmap.
 *
 * @param scale - The scale factor for resizing the image before blurring.
 * @param radius - The radius of the square used for blurring.
 * @return A new Bitmap with the blur effect applied.
 *
 * The algorithm is a blur transformation that applies a Gaussian blur effect to an input bitmap image.
 * It works by computing the average color value of each pixel in a radius around the pixel.
 * The radius of the blur effect is specified by the radius parameter.
 *
 * The algorithm uses a stack to keep track of the color values of the pixels in the blur radius.
 * It iterates over each pixel in the image and computes the weighted sum of the red, green, and blue color values
 * of the pixels in the blur radius.
 *
 * This sum is then divided by the sum of the divisor values to obtain the average color value for the pixel.
 */
private suspend fun Bitmap.blur(
    scale: Float,
    radius: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    if (radius < 1) {
        return@withContext null
    }

    // Scale the bitmap image.
    var scaledBitmap = this@blur
    val scaledWidth = (scaledBitmap.width * scale).roundToInt()
    val scaledHeight = (scaledBitmap.height * scale).roundToInt()
    scaledBitmap = Bitmap.createScaledBitmap(scaledBitmap, scaledWidth, scaledHeight, false)

    val bitmap = scaledBitmap.copy(scaledBitmap.config, true)

    // Initialize variables for the blur algorithm.
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    val widthMinusOne = width - 1
    val heightMinusOne = height - 1
    val imagePixelCount = width * height
    val totalPixelsToAverage = radius + radius + 1
    val reds = IntArray(imagePixelCount)
    val greens = IntArray(imagePixelCount)
    val blues = IntArray(imagePixelCount)
    var redSum: Int
    var greenSum: Int
    var blueSum: Int
    var x: Int
    var y: Int
    var i: Int
    var pixelIndex: Int
    var yOffset: Int
    var rowStartIndex: Int
    val minimumDimension = IntArray(width.coerceAtLeast(height))
    var divisorSum = totalPixelsToAverage + 1 shr 1
    divisorSum *= divisorSum
    val weightedColorSum = IntArray(256 * divisorSum)
    i = 0
    while (i < 256 * divisorSum) {
        weightedColorSum[i] = i / divisorSum
        i++
    }
    rowStartIndex = 0
    var yw = 0
    val stack = Array(totalPixelsToAverage) {
        IntArray(3)
    }
    var stackPointer: Int
    var stackStart: Int
    var currentPixel: IntArray
    var radiusMinusAbsolute: Int
    val radiusPlusOne = radius + 1
    var redOutSum: Int
    var greenOutSum: Int
    var blueOutSum: Int
    var redInSum: Int
    var greenInSum: Int
    var blueInSum: Int

    // Apply the blur algorithm to each row of pixels in the bitmap image.
    y = 0
    while (y < height) {
        blueSum = 0
        greenSum = 0
        redSum = 0
        blueOutSum = 0
        greenOutSum = 0
        redOutSum = 0
        blueInSum = 0
        greenInSum = 0
        redInSum = 0
        i = -radius
        while (i <= radius) {
            pixelIndex = pixels[rowStartIndex + (i.coerceIn(0, widthMinusOne))]
            currentPixel = stack[i + radius]
            currentPixel[0] = pixelIndex and 0xff0000 shr 16
            currentPixel[1] = pixelIndex and 0x00ff00 shr 8
            currentPixel[2] = pixelIndex and 0x0000ff
            radiusMinusAbsolute = radiusPlusOne - abs(i)
            redSum += currentPixel[0] * radiusMinusAbsolute
            greenSum += currentPixel[1] * radiusMinusAbsolute
            blueSum += currentPixel[2] * radiusMinusAbsolute
            if (i > 0) {
                redInSum += currentPixel[0]
                greenInSum += currentPixel[1]
                blueInSum += currentPixel[2]
            } else {
                redOutSum += currentPixel[0]
                greenOutSum += currentPixel[1]
                blueOutSum += currentPixel[2]
            }
            i++
        }
        stackPointer = radius
        x = 0
        while (x < width) {
            reds[rowStartIndex] = weightedColorSum[redSum]
            greens[rowStartIndex] = weightedColorSum[greenSum]
            blues[rowStartIndex] = weightedColorSum[blueSum]
            redSum -= redOutSum
            greenSum -= greenOutSum
            blueSum -= blueOutSum
            stackStart = stackPointer - radius + totalPixelsToAverage
            currentPixel = stack[stackStart % totalPixelsToAverage]
            redOutSum -= currentPixel[0]
            greenOutSum -= currentPixel[1]
            blueOutSum -= currentPixel[2]
            if (y == 0) {
                minimumDimension[x] = (x + radius + 1).coerceAtMost(widthMinusOne)
            }
            pixelIndex = pixels[yw + minimumDimension[x]]
            currentPixel[0] = pixelIndex and 0xff0000 shr 16
            currentPixel[1] = pixelIndex and 0x00ff00 shr 8
            currentPixel[2] = pixelIndex and 0x0000ff
            redInSum += currentPixel[0]
            greenInSum += currentPixel[1]
            blueInSum += currentPixel[2]
            redSum += redInSum
            greenSum += greenInSum
            blueSum += blueInSum
            stackPointer = (stackPointer + 1) % totalPixelsToAverage
            currentPixel = stack[stackPointer % totalPixelsToAverage]
            redOutSum += currentPixel[0]
            greenOutSum += currentPixel[1]
            blueOutSum += currentPixel[2]
            redInSum -= currentPixel[0]
            greenInSum -= currentPixel[1]
            blueInSum -= currentPixel[2]
            rowStartIndex++
            x++
        }
        yw += width
        y++
    }

    // Apply the blur algorithm to each column of pixels in the bitmap image.
    x = 0
    while (x < width) {
        blueSum = 0
        greenSum = blueSum
        redSum = greenSum
        blueOutSum = redSum
        greenOutSum = blueOutSum
        redOutSum = greenOutSum
        blueInSum = redOutSum
        greenInSum = blueInSum
        redInSum = greenInSum
        yOffset = -radius * width
        i = -radius
        while (i <= radius) {
            rowStartIndex = 0.coerceAtLeast(yOffset) + x
            currentPixel = stack[i + radius]
            currentPixel[0] = reds[rowStartIndex]
            currentPixel[1] = greens[rowStartIndex]
            currentPixel[2] = blues[rowStartIndex]
            radiusMinusAbsolute = radiusPlusOne - abs(i)
            redSum += reds[rowStartIndex] * radiusMinusAbsolute
            greenSum += greens[rowStartIndex] * radiusMinusAbsolute
            blueSum += blues[rowStartIndex] * radiusMinusAbsolute
            if (i > 0) {
                redInSum += currentPixel[0]
                greenInSum += currentPixel[1]
                blueInSum += currentPixel[2]
            } else {
                redOutSum += currentPixel[0]
                greenOutSum += currentPixel[1]
                blueOutSum += currentPixel[2]
            }
            if (i < heightMinusOne) {
                yOffset += width
            }
            i++
        }
        rowStartIndex = x
        stackPointer = radius
        y = 0
        while (y < height) {
            // Set the blurred pixel color in the bitmap image.
            pixels[rowStartIndex] = -0x1000000 and pixels[rowStartIndex] or
                (weightedColorSum[redSum] shl 16) or
                (weightedColorSum[greenSum] shl 8) or
                weightedColorSum[blueSum]

            redSum -= redOutSum
            greenSum -= greenOutSum
            blueSum -= blueOutSum
            stackStart = stackPointer - radius + totalPixelsToAverage
            currentPixel = stack[stackStart % totalPixelsToAverage]
            redOutSum -= currentPixel[0]
            greenOutSum -= currentPixel[1]
            blueOutSum -= currentPixel[2]
            if (x == 0) {
                minimumDimension[y] = (y + radiusPlusOne).coerceAtMost(heightMinusOne) * width
            }
            pixelIndex = x + minimumDimension[y]
            currentPixel[0] = reds[pixelIndex]
            currentPixel[1] = greens[pixelIndex]
            currentPixel[2] = blues[pixelIndex]
            redInSum += currentPixel[0]
            greenInSum += currentPixel[1]
            blueInSum += currentPixel[2]
            redSum += redInSum
            greenSum += greenInSum
            blueSum += blueInSum
            stackPointer = (stackPointer + 1) % totalPixelsToAverage
            currentPixel = stack[stackPointer]
            redOutSum += currentPixel[0]
            greenOutSum += currentPixel[1]
            blueOutSum += currentPixel[2]
            redInSum -= currentPixel[0]
            greenInSum -= currentPixel[1]
            blueInSum -= currentPixel[2]
            rowStartIndex += width
            y++
        }
        x++
    }

    // Set the blurred pixels in the bitmap image.
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    // Recycle the scaled bitmap image.
    scaledBitmap.recycle()

    // Return the blurred bitmap image.
    return@withContext bitmap
}