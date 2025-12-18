package au.com.gman.bottlerocket.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.applyRotation
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.extensions.toPath
import au.com.gman.bottlerocket.interfaces.IBitmapRescaler
import au.com.gman.bottlerocket.interfaces.IImageEnhancer
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ImageEnhancer @Inject constructor(
    private val bitmapRescaler: IBitmapRescaler,
    private val screenDimensions: IScreenDimensions
) : IImageEnhancer {

    companion object {
        private const val TAG = "ImageEnhancer"
    }

    override fun processImageWithMatchedTemplate(
        bitmap: Bitmap,
        detectionResult: BarcodeDetectionResult
    ): Bitmap? {

        if (!detectionResult.matchFound ||
            detectionResult.pageOverlayPath == null ||
            detectionResult.pageTemplate == null
        ) {
            return null
        }

        // Rotate the bitmap first
        val rotatedBitmap = bitmap.rotate(detectionResult.cameraRotation)

        // IMPORTANT: These are the ImageAnalysis dimensions where barcodes were detected
        val imageAnalysisSize = PointF(1200f, 1600f)  // The actual analysis resolution

        Log.d(TAG, "ImageAnalysis dimensions: ${imageAnalysisSize.x}x${imageAnalysisSize.y}")
        Log.d(TAG, "Rotated bitmap: ${rotatedBitmap.width}x${rotatedBitmap.height}")

        // Calculate scaling factors from ImageAnalysis coordinates to bitmap coordinates
        val scaleX = rotatedBitmap.width.toFloat() / imageAnalysisSize.x
        val scaleY = rotatedBitmap.height.toFloat() / imageAnalysisSize.y

        Log.d(TAG, "Scale factors: X=$scaleX, Y=$scaleY")

        // Scale the overlay from ImageAnalysis coordinates to bitmap coordinates
        val scaledOverlay = detectionResult.pageOverlayPath.scaleUpWithOffset(
            ScaleAndOffset(
                PointF(scaleX, scaleY),
                PointF(0F, 0F)
            )
        )

        val scaledQrOverlay = detectionResult.qrCodeOverlayPath?.scaleUpWithOffset(
            ScaleAndOffset(
                PointF(scaleX, scaleY),
                PointF(0F, 0F)
            )
        )

        Log.d(TAG, "Original overlay: ${detectionResult.pageOverlayPath}")
        Log.d(TAG, "Scaled overlay: $scaledOverlay")

        // Apply rotation if needed
        val finalOverlay = if (detectionResult.boundingBoxRotation != 0f) {
            scaledOverlay.applyRotation(
                -detectionResult.boundingBoxRotation,
                PointF(
                    rotatedBitmap.width.toFloat() / 2f,
                    rotatedBitmap.height.toFloat() / 2f
                )
            )
        } else {
            scaledOverlay
        }

        val finalQrOverlay = if (detectionResult.boundingBoxRotation != 0f) {
            scaledQrOverlay?.applyRotation(
                -detectionResult.boundingBoxRotation,
                PointF(
                    rotatedBitmap.width.toFloat() / 2f,
                    rotatedBitmap.height.toFloat() / 2f
                )
            )
        } else {
            scaledQrOverlay
        }

        val enhancedBitmap = rotatedBitmap.enhanceImage(
            finalQrOverlay,
            finalOverlay
        )

        return enhancedBitmap
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.enhanceImage(
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

        var debugPaintRed = Paint()
            .apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }

        var debugPaintGreen = Paint()
            .apply {
                color = Color.GREEN
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }

        if (pageBoundingBoxUnscaled != null)
            canvas.drawPath(pageBoundingBoxUnscaled.toPath(), debugPaintRed)

        if (pageBoundingBoxScaled != null)
            canvas.drawPath(pageBoundingBoxScaled.toPath(), debugPaintGreen)

        return enhanced
    }

    fun Bitmap.extractPageWithPerspective(
        pageBounds: RocketBoundingBox,
        outputWidth: Int,
        outputHeight: Int
    ): Bitmap {
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
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw the transformed bitmap
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(this, matrix, paint)

        return output
    }

    /**
     * Automatically calculates output dimensions to maintain aspect ratio.
     * Uses the average width/height from the bounding box.
     */
    fun Bitmap.extractPageWithPerspective(
        pageBounds: RocketBoundingBox,
        maxDimension: Int = 2048
    ): Bitmap {
        // Calculate average dimensions from the bounding box
        val topWidth = distance(pageBounds.topLeft, pageBounds.topRight)
        val bottomWidth = distance(pageBounds.bottomLeft, pageBounds.bottomRight)
        val leftHeight = distance(pageBounds.topLeft, pageBounds.bottomLeft)
        val rightHeight = distance(pageBounds.topRight, pageBounds.bottomRight)

        val avgWidth = (topWidth + bottomWidth) / 2f
        val avgHeight = (leftHeight + rightHeight) / 2f

        // Scale to fit within maxDimension while maintaining aspect ratio
        val scale = if (avgWidth > avgHeight) {
            maxDimension / avgWidth
        } else {
            maxDimension / avgHeight
        }

        val outputWidth = (avgWidth * scale).roundToInt()
        val outputHeight = (avgHeight * scale).roundToInt()

        return extractPageWithPerspective(pageBounds, outputWidth, outputHeight)
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
}