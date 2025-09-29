package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.graphics.Bitmap

// This string is used for error messages.
private const val ExternalName = "RenderScript Toolkit"

/**
 * https://github.com/skydoves/landscapist/blob/main/landscapist-transformation/src/main/kotlin/com/skydoves/landscapist/transformation/RenderScriptToolkit.kt
 *
 * A collection of high-performance graphic utility functions like blur and blend.
 *
 * This toolkit provides ten image manipulation functions: blend, blur, color matrix, convolve,
 * histogram, histogramDot, lut, lut3d, resize, and YUV to RGB. These functions execute
 * multithreaded on the CPU.
 *
 * Most of the functions have two variants: one that manipulates Bitmaps, the other ByteArrays.
 * For ByteArrays, you need to specify the width and height of the data to be processed, as
 * well as the number of bytes per pixel. For most use cases, this will be 4.
 *
 * The Toolkit creates a thread pool that's used for processing the functions. The threads live
 * for the duration of the application. They can be destroyed by calling the method shutdown().
 *
 * This library is thread safe. You can call methods from different poolThreads. The functions will
 * execute sequentially.
 *
 * A native C++ version of this Toolkit is available. Check the RenderScriptToolkit.h file in the
 * cpp directory.
 *
 * This toolkit can be used as a replacement for most RenderScript Intrinsic functions. Compared
 * to RenderScript, it's simpler to use and more than twice as fast on the CPU. However RenderScript
 * Intrinsics allow more flexibility for the type of allocation supported. In particular, this
 * toolkit does not support allocations of floats.
 */
internal object RenderScriptToolkit {

    /**
     * Blurs an image.
     *
     * Performs a Gaussian blur of a Bitmap and returns result as a Bitmap. A variant of
     * this method is available to blur ByteArrays.
     *
     * The radius determines which pixels are used to compute each blurred pixels. This Toolkit
     * accepts values between 1 and 25. Larger values create a more blurred effect but also
     * take longer to compute. When the radius extends past the edge, the edge pixel will
     * be used as replacement for the pixel that's out off boundary.
     *
     * This method supports input Bitmap of config ARGB_8888 and ALPHA_8. Bitmaps with a stride
     * different than width * vectorSize are not currently supported. The returned Bitmap has the
     * same config.
     *
     * An optional range parameter can be set to restrict the operation to a rectangular subset
     * of each buffer. If provided, the range must be wholly contained with the dimensions
     * described by sizeX and sizeY. NOTE: The output Bitmap will still be full size, with the
     * section that's not blurred all set to 0. This is to stay compatible with RenderScript.
     *
     * @param inputBitmap The buffer of the image to be blurred.
     * @return The blurred Bitmap.
     */
    @Suppress("MagicNumber", "ReturnCount")
    @JvmOverloads
    internal fun blur(
        inputBitmap: Bitmap?,
        @androidx.annotation.IntRange(from = 0, to = 25) radius: Int = 5,
        restriction: Range2d? = null,
    ): Bitmap? {
        inputBitmap ?: return null
        validateBitmap("blur", inputBitmap)
        if (radius == 0) return inputBitmap
        require(radius in 1..25) {
            "$ExternalName blur. The radius should be between 1 and 25. $radius provided."
        }
        validateRestriction("blur", inputBitmap.width, inputBitmap.height, restriction)

        val outputBitmap = createCompatibleBitmap(inputBitmap)
        nativeBlurBitmap(nativeHandle, inputBitmap, outputBitmap, radius, restriction)
        return outputBitmap
    }

    private var nativeHandle: Long = 0

    init {
        System.loadLibrary("renderscript-toolkit")
        nativeHandle = createNative()
    }

    private external fun createNative(): Long

    private external fun nativeBlurBitmap(
        nativeHandle: Long,
        inputBitmap: Bitmap,
        outputBitmap: Bitmap,
        radius: Int,
        restriction: Range2d?,
    )
}

/**
 * Define a range of data to process.
 *
 * This class is used to restrict a [RenderScriptToolkit] operation to a rectangular subset of the input
 * tensor.
 *
 * @property startX The index of the first value to be included on the X axis.
 * @property endX The index after the last value to be included on the X axis.
 * @property startY The index of the first value to be included on the Y axis.
 * @property endY The index after the last value to be included on the Y axis.
 */
internal data class Range2d(
    val startX: Int,
    val endX: Int,
    val startY: Int,
    val endY: Int,
)

internal fun validateBitmap(
    function: String,
    inputBitmap: Bitmap,
    alphaAllowed: Boolean = true,
) {
    if (alphaAllowed) {
        require(
            inputBitmap.config == Bitmap.Config.ARGB_8888 || inputBitmap.config == Bitmap.Config.ALPHA_8,
        ) {
            "$ExternalName. $function supports only ARGB_8888 and ALPHA_8 bitmaps. " + "${inputBitmap.config} provided."
        }
    } else {
        require(inputBitmap.config == Bitmap.Config.ARGB_8888) {
            "$ExternalName. $function supports only ARGB_8888. " + "${inputBitmap.config} provided."
        }
    }
    require(inputBitmap.width * vectorSize(inputBitmap) == inputBitmap.rowBytes) {
        "$ExternalName $function. Only bitmaps with rowSize equal to the width * vectorSize are " +
            "currently supported. Provided were rowBytes=${inputBitmap.rowBytes}, " +
            "width={${inputBitmap.width}, and vectorSize=${
                vectorSize(
                    inputBitmap,
                )
            }."
    }
}

internal fun createCompatibleBitmap(inputBitmap: Bitmap) =
    Bitmap.createBitmap(inputBitmap.width, inputBitmap.height, inputBitmap.config!!)

internal fun validateRestriction(
    tag: String,
    sizeX: Int,
    sizeY: Int,
    restriction: Range2d? = null,
) {
    if (restriction == null) return
    require(restriction.startX < sizeX && restriction.endX <= sizeX) {
        "$ExternalName $tag. sizeX should be greater than restriction.startX and greater " +
            "or equal to restriction.endX. $sizeX, ${restriction.startX}, " +
            "and ${restriction.endX} were provided respectively."
    }
    require(restriction.startY < sizeY && restriction.endY <= sizeY) {
        "$ExternalName $tag. sizeY should be greater than restriction.startY and greater " +
            "or equal to restriction.endY. $sizeY, ${restriction.startY}, " +
            "and ${restriction.endY} were provided respectively."
    }
    require(restriction.startX < restriction.endX) {
        "$ExternalName $tag. Restriction startX should be less than endX. " +
            "${restriction.startX} and ${restriction.endX} were provided respectively."
    }
    require(restriction.startY < restriction.endY) {
        "$ExternalName $tag. Restriction startY should be less than endY. " +
            "${restriction.startY} and ${restriction.endY} were provided respectively."
    }
}

@Suppress("MagicNumber")
internal fun vectorSize(bitmap: Bitmap): Int {
    return when (bitmap.config) {
        Bitmap.Config.ARGB_8888 -> 4
        Bitmap.Config.ALPHA_8 -> 1
        else -> throw IllegalArgumentException(
            "$ExternalName. Only ARGB_8888 and ALPHA_8 Bitmap are supported.",
        )
    }
}
