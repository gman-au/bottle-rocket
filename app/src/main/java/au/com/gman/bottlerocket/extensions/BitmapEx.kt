package au.com.gman.bottlerocket.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt


fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.enhanceImage(
    pageBoundingBoxUnscaled: RocketBoundingBox? = null,
    pageBoundingBoxScaled: RocketBoundingBox? = null,
): Bitmap {

    val enhanced =
        this
            .copy(Bitmap.Config.ARGB_8888, true)

    val contrastFactor = 1.2f
    val offset = 0f

    val cm = ColorMatrix(
        floatArrayOf(
            contrastFactor, 0f, 0f, 0f, offset,
            0f, contrastFactor, 0f, 0f, offset,
            0f, 0f, contrastFactor, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(cm)

    val canvas = Canvas(enhanced)

    canvas
        .drawBitmap(
            this,
            0f,
            0f,
            paint
        )

    val debugPaintRed = Paint()
        .apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

    val debugPaintGreen = Paint()
        .apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

    if (pageBoundingBoxUnscaled != null)
        canvas
            .drawPath(
                pageBoundingBoxUnscaled.toPath(),
                debugPaintRed
            )

    if (pageBoundingBoxScaled != null)
        canvas
            .drawPath(
                pageBoundingBoxScaled.toPath(),
                debugPaintGreen
            )

    return enhanced
}

fun Bitmap.cropToPageBounds(pageBounds: RocketBoundingBox): Bitmap {
    // Calculate average dimensions from the bounding box
    val topWidth = distance(pageBounds.topLeft, pageBounds.topRight)
    val bottomWidth = distance(pageBounds.bottomLeft, pageBounds.bottomRight)
    val leftHeight = distance(pageBounds.topLeft, pageBounds.bottomLeft)
    val rightHeight = distance(pageBounds.topRight, pageBounds.bottomRight)

    val avgWidth = (topWidth + bottomWidth) / 2f
    val avgHeight = (leftHeight + rightHeight) / 2f

    // Scale to fit within maxDimension while maintaining aspect ratio
    val maxDimension = 2048
    val scale = if (avgWidth > avgHeight) {
        maxDimension / avgWidth
    } else {
        maxDimension / avgHeight
    }

    val outputWidth = (avgWidth * scale).roundToInt()
    val outputHeight = (avgHeight * scale).roundToInt()

    // Source corners (the distorted quadrilateral in the original image)
    val srcCorners = floatArrayOf(
        pageBounds.topLeft.x, pageBounds.topLeft.y,
        pageBounds.topRight.x, pageBounds.topRight.y,
        pageBounds.bottomRight.x, pageBounds.bottomRight.y,
        pageBounds.bottomLeft.x, pageBounds.bottomLeft.y
    )

    // Destination corners (rectangle in output image)
    val dstCorners = floatArrayOf(
        0f, 0f,                             // top-left
        outputWidth.toFloat(), 0f,          // top-right
        outputWidth.toFloat(), outputHeight.toFloat(), // bottom-right
        0f, outputHeight.toFloat()          // bottom-left
    )

    // Create transformation matrix
    val matrix = Matrix()
    matrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

    // Create output bitmap
    val output = createBitmap(outputWidth, outputHeight)
    val canvas = Canvas(output)

    // Draw the transformed bitmap
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    canvas.drawBitmap(this, matrix, paint)

    return output
}

private fun distance(p1: PointF, p2: PointF): Float {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    return sqrt(dx * dx + dy * dy)
}
